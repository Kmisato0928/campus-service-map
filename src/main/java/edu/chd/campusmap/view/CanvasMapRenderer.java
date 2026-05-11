package edu.chd.campusmap.view;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CanvasMapRenderer extends Pane {
    private static final int TILE_SIZE = 256;
    private static final String TILE_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";
    private static final Path DISK_CACHE = Paths.get(
            System.getProperty("java.io.tmpdir"), "campusmap-tiles");

    private final Canvas canvas;
    private double centerLat = 34.3740;
    private double centerLon = 108.9100;
    private int zoom = 15;

    private double offsetX = 0;
    private double offsetY = 0;

    private final Map<String, Image> tileCache = new HashMap<>();
    private final Set<String> pendingLoads = ConcurrentHashMap.newKeySet();
    private final ExecutorService tileLoader = Executors.newFixedThreadPool(4);

    private double dragStartX, dragStartY;
    private boolean isDragging = false;

    // Building drag mode
    private boolean dragModeActive = false;
    private int dragBuildingId = -1;
    private BuildingMarker dragTarget = null;
    private double dragVisualLat, dragVisualLon;
    private BiConsumer<Double, Double> onBuildingDrag;

    // Smooth animation
    private AnimationTimer currentAnimation;

    private final List<BuildingMarker> markers = new ArrayList<>();
    private Consumer<BuildingMarker> onMarkerClick;

    public static class BuildingMarker {
        public final int id;
        public final String name;
        public final double lat;
        public final double lon;
        public final String category;
        public final Color color;

        public BuildingMarker(int id, String name, double lat, double lon, String category, Color color) {
            this.id = id;
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.category = category;
            this.color = color;
        }
    }

    public CanvasMapRenderer() {
        this.canvas = new Canvas();
        getChildren().add(canvas);

        // 尺寸变化时自动调整 Canvas 缓冲区并重绘
        widthProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() > 0) resizeCanvas();
        });
        heightProperty().addListener((obs, o, n) -> {
            if (n.doubleValue() > 0) resizeCanvas();
        });

        setupMouseHandlers();
        javafx.application.Platform.runLater(this::render);

        // 注册 JVM 关闭钩子，确保退出时清理磁盘缓存
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            tileLoader.shutdown();
            cleanDiskCache();
        }));
    }

    private void resizeCanvas() {
        double w = getWidth();
        double h = getHeight();
        if (w > 0 && h > 0) {
            canvas.setWidth(w);
            canvas.setHeight(h);
        }
        render();
    }

    private GraphicsContext gc() {
        return canvas.getGraphicsContext2D();
    }

    private void setupMouseHandlers() {
        setOnMousePressed(e -> {
            if (dragModeActive) {
                BuildingMarker clicked = findMarkerAt(e.getX(), e.getY());
                if (clicked != null && clicked.id == dragBuildingId) {
                    dragTarget = clicked;
                    dragVisualLat = clicked.lat;
                    dragVisualLon = clicked.lon;
                    return;
                }
                // In drag mode, clicking on a different marker does nothing;
                // clicking empty space falls through to allow map panning
            }

            BuildingMarker clicked = findMarkerAt(e.getX(), e.getY());
            if (clicked != null && onMarkerClick != null) {
                onMarkerClick.accept(clicked);
                return;
            }
            dragStartX = e.getX();
            dragStartY = e.getY();
            isDragging = true;
        });

        setOnMouseDragged(e -> {
            if (dragModeActive && dragTarget != null) {
                double[] latLon = screenToLatLon(e.getX(), e.getY());
                dragVisualLat = latLon[0];
                dragVisualLon = latLon[1];
                if (onBuildingDrag != null) {
                    onBuildingDrag.accept(latLon[0], latLon[1]);
                }
                render();
                return;
            }

            if (isDragging) {
                double dx = e.getX() - dragStartX;
                double dy = e.getY() - dragStartY;
                offsetX += dx;
                offsetY += dy;
                dragStartX = e.getX();
                dragStartY = e.getY();
                render();
            }
        });

        setOnMouseReleased(e -> {
            if (dragModeActive) {
                dragTarget = null;
            }
            isDragging = false;
        });

        setOnScroll(e -> {
            int oldZoom = zoom;
            if (e.getDeltaY() > 0 && zoom < 19) {
                zoom++;
            } else if (e.getDeltaY() < 0 && zoom > 1) {
                zoom--;
            }
            if (oldZoom != zoom) {
                double currentCenterTileX = lon2tileExact(centerLon, oldZoom) + offsetX / TILE_SIZE;
                double currentCenterTileY = lat2tileExact(centerLat, oldZoom) + offsetY / TILE_SIZE;
                centerLon = tile2lon(currentCenterTileX, oldZoom);
                centerLat = tile2lat(currentCenterTileY, oldZoom);
                offsetX = 0;
                offsetY = 0;
                tileCache.clear();
                pendingLoads.clear();
                System.out.println("[Zoom] " + oldZoom + " -> " + zoom + " | center=(" + centerLat + ", " + centerLon + ")");
                render();
            }
        });
    }

    private void render() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        // 确保 Canvas 缓冲区尺寸匹配
        if (canvas.getWidth() != w || canvas.getHeight() != h) {
            canvas.setWidth(w);
            canvas.setHeight(h);
        }

        GraphicsContext g = gc();
        g.setFill(Color.web("#f0f0f0"));
        g.fillRect(0, 0, w, h);

        int centerTileX = lon2tile(centerLon, zoom);
        int centerTileY = lat2tile(centerLat, zoom);

        // 根据 offset 动态计算需要渲染的瓦片范围，offset 任意大时都能覆盖可视区域
        int halfW = (int) Math.ceil(w / 2 / TILE_SIZE) + 1;
        int halfH = (int) Math.ceil(h / 2 / TILE_SIZE) + 1;

        int dxMin = -halfW - (offsetX > 0 ? (int) Math.ceil(offsetX / TILE_SIZE) : 0);
        int dxMax = halfW + (offsetX < 0 ? (int) Math.ceil(-offsetX / TILE_SIZE) : 0);
        int dyMin = -halfH - (offsetY > 0 ? (int) Math.ceil(offsetY / TILE_SIZE) : 0);
        int dyMax = halfH + (offsetY < 0 ? (int) Math.ceil(-offsetY / TILE_SIZE) : 0);

        for (int dx = dxMin; dx <= dxMax; dx++) {
            for (int dy = dyMin; dy <= dyMax; dy++) {
                int tileX = centerTileX + dx;
                int tileY = centerTileY + dy;
                if (tileX < 0 || tileY < 0 || tileX >= (1 << zoom) || tileY >= (1 << zoom)) {
                    continue;
                }

                double screenX = w / 2 + dx * TILE_SIZE + offsetX;
                double screenY = h / 2 + dy * TILE_SIZE + offsetY;

                String key = zoom + "/" + tileX + "/" + tileY;
                Image tile = tileCache.get(key);

                if (tile != null) {
                    g.drawImage(tile, screenX, screenY);
                } else {
                    g.setFill(Color.web("#e0e0e0"));
                    g.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
                    if (pendingLoads.add(key)) {
                        loadTileAsync(tileX, tileY, zoom, key);
                    }
                }
            }
        }

        renderMarkers(g);
    }

    /**
     * 平滑动画：将地图中心平滑移动到目标经纬度，使用 ease-out 缓动
     */
    public void animateCenter(double targetLat, double targetLon) {
        if (currentAnimation != null) {
            currentAnimation.stop();
            currentAnimation = null;
        }

        // 将当前偏移量"烘烤"到中心坐标中，使动画起始帧无跳变
        double virtualTileX = lon2tileExact(centerLon, zoom) + offsetX / TILE_SIZE;
        double virtualTileY = lat2tileExact(centerLat, zoom) + offsetY / TILE_SIZE;
        double startLat = tile2lat(virtualTileY, zoom);
        double startLon = tile2lon(virtualTileX, zoom);

        // First frame: move center to virtual center, clear offset (no visual jump)
        centerLat = startLat;
        centerLon = startLon;
        offsetX = 0;
        offsetY = 0;

        double dLat = targetLat - startLat;
        double dLon = targetLon - startLon;
        // 经度归一化，防止绕远路
        if (dLon > 180) dLon -= 360;
        else if (dLon < -180) dLon += 360;

        final double fStartLat = startLat;
        final double fStartLon = startLon;
        final double fDlat = dLat;
        final double fDlon = dLon;
        final long duration = 400_000_000L; // 400ms

        currentAnimation = new AnimationTimer() {
            final long startTime = System.nanoTime();

            @Override
            public void handle(long now) {
                double t = Math.min(1.0, (now - startTime) / (double) duration);
                t = 1 - (1 - t) * (1 - t); // ease-out quad
                centerLat = fStartLat + fDlat * t;
                centerLon = fStartLon + fDlon * t;
                offsetX = 0;
                offsetY = 0;
                render();
                if (t >= 1.0) {
                    centerLat = targetLat;
                    centerLon = targetLon;
                    stop();
                    currentAnimation = null;
                }
            }
        };
        currentAnimation.start();
    }

    private void renderMarkers(GraphicsContext g) {
        for (BuildingMarker m : markers) {
            double[] screen;
            if (dragModeActive && m == dragTarget) {
                screen = latLonToScreen(dragVisualLat, dragVisualLon);
            } else {
                screen = latLonToScreen(m.lat, m.lon);
            }
            if (screen == null) continue;

            if (dragModeActive) {
                g.setFill(Color.RED);
                g.fillOval(screen[0] - 10, screen[1] - 10, 20, 20);
                g.setStroke(Color.YELLOW);
                g.setLineWidth(3);
                g.strokeOval(screen[0] - 10, screen[1] - 10, 20, 20);
            } else {
                g.setFill(m.color);
                g.fillOval(screen[0] - 8, screen[1] - 8, 16, 16);
                g.setStroke(Color.WHITE);
                g.setLineWidth(3);
                g.strokeOval(screen[0] - 8, screen[1] - 8, 16, 16);
            }
        }
    }

    private double[] latLonToScreen(double lat, double lon) {
        double tileX = lon2tileExact(lon, zoom);
        double tileY = lat2tileExact(lat, zoom);
        double centerTileX = lon2tileExact(centerLon, zoom);
        double centerTileY = lat2tileExact(centerLat, zoom);

        double pixelX = (tileX - centerTileX) * TILE_SIZE + getWidth() / 2 + offsetX;
        double pixelY = (tileY - centerTileY) * TILE_SIZE + getHeight() / 2 + offsetY;

        return new double[]{pixelX, pixelY};
    }

    private double[] screenToLatLon(double screenX, double screenY) {
        double centerTileX = lon2tileExact(centerLon, zoom);
        double centerTileY = lat2tileExact(centerLat, zoom);

        double tileX = (screenX - getWidth() / 2 - offsetX) / TILE_SIZE + centerTileX;
        double tileY = (screenY - getHeight() / 2 - offsetY) / TILE_SIZE + centerTileY;

        double lon = tileX / (1 << zoom) * 360.0 - 180.0;
        double n = Math.PI - 2.0 * Math.PI * tileY / (1 << zoom);
        double lat = Math.toDegrees(Math.atan(Math.sinh(n)));

        return new double[]{lat, lon};
    }

    private double lon2tileExact(double lon, int zoom) {
        return (lon + 180.0) / 360.0 * (1 << zoom);
    }

    private double lat2tileExact(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 << zoom);
    }

    private BuildingMarker findMarkerAt(double x, double y) {
        for (BuildingMarker m : markers) {
            double[] screen;
            if (dragModeActive && m == dragTarget) {
                screen = latLonToScreen(dragVisualLat, dragVisualLon);
            } else {
                screen = latLonToScreen(m.lat, m.lon);
            }
            if (screen == null) continue;
            double dist = Math.sqrt(Math.pow(x - screen[0], 2) + Math.pow(y - screen[1], 2));
            if (dist <= (dragModeActive ? 15 : 12)) return m;
        }
        return null;
    }

    /**
     * 异步加载瓦片：磁盘缓存 → 网络请求，无论如何最终会存入内存缓存。
     * 磁盘缓存避免同一瓦片在本次运行中被重复请求。
     */
    private void loadTileAsync(int tileX, int tileY, int z, String key) {
        tileLoader.submit(() -> {
            try {
                Image img = loadTileFromDisk(tileX, tileY, z);
                if (img == null) {
                    img = fetchTileFromNetwork(tileX, tileY, z);
                }
                if (img != null) {
                    synchronized (tileCache) {
                        tileCache.put(key, img);
                    }
                    if (z == zoom) {
                        javafx.application.Platform.runLater(this::render);
                    }
                }
            } catch (Exception e) {
                System.err.println("[Tile] 加载失败: " + key + " - " + e.getMessage());
            } finally {
                pendingLoads.remove(key);
            }
        });
    }

    private Image loadTileFromDisk(int x, int y, int z) {
        try {
            Path path = DISK_CACHE.resolve(z + "/" + x + "/" + y + ".png");
            if (Files.exists(path)) {
                return new Image(Files.newInputStream(path));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Image fetchTileFromNetwork(int x, int y, int z) throws Exception {
        String urlStr = TILE_URL
                .replace("{z}", String.valueOf(z))
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y));

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7897));
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection(proxy);
        conn.setRequestProperty("User-Agent", "CampusMapSystem/1.0");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        try (InputStream is = conn.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            // 存到磁盘缓存
            try {
                Path path = DISK_CACHE.resolve(z + "/" + x + "/" + y + ".png");
                Files.createDirectories(path.getParent());
                Files.write(path, bytes);
            } catch (Exception e) {
                System.err.println("[Cache] 写入失败: " + z + "/" + x + "/" + y);
            }
            return new Image(new ByteArrayInputStream(bytes));
        }
    }

    // ---- 拖动模式接口 ----

    public void enterDragMode(int buildingId, BiConsumer<Double, Double> onDrag) {
        this.dragBuildingId = buildingId;
        this.dragModeActive = true;
        this.onBuildingDrag = onDrag;
        render();
    }

    public void exitDragMode() {
        this.dragModeActive = false;
        this.dragBuildingId = -1;
        this.dragTarget = null;
        this.onBuildingDrag = null;
        render();
    }

    public boolean isDragModeActive() {
        return dragModeActive;
    }

    // ---- 原有公共方法 ----

    private int lon2tile(double lon, int zoom) {
        return (int) Math.floor((lon + 180.0) / 360.0 * (1 << zoom));
    }

    private int lat2tile(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (int) Math.floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 << zoom));
    }

    public void addMarker(int id, String name, double lat, double lon, String category, Color color) {
        markers.add(new BuildingMarker(id, name, lat, lon, category, color));
        render();
    }

    public void clearMarkers() {
        markers.clear();
        render();
    }

    public void setOnMarkerClick(Consumer<BuildingMarker> callback) {
        this.onMarkerClick = callback;
    }

    public void setCenter(double lat, double lon) {
        this.centerLat = lat;
        this.centerLon = lon;
        this.offsetX = 0;
        this.offsetY = 0;
        render();
    }

    public void setZoom(int zoom) {
        this.zoom = Math.max(1, Math.min(19, zoom));
        this.offsetX = 0;
        this.offsetY = 0;
        tileCache.clear();
        pendingLoads.clear();
        render();
    }

    private double tile2lon(double x, int zoom) {
        return x / (1 << zoom) * 360.0 - 180.0;
    }

    private double tile2lat(double y, int zoom) {
        double n = Math.PI - 2.0 * Math.PI * y / (1 << zoom);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }

    public void shutdown() {
        tileLoader.shutdown();
        cleanDiskCache();
    }

    private void cleanDiskCache() {
        try (var stream = Files.walk(DISK_CACHE)) {
            stream.sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (Exception ignored) {}
                    });
        } catch (Exception ignored) {
        }
    }
}
