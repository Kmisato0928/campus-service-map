package edu.chd.campusmap.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class CanvasMapRenderer extends Canvas {
    private static final int TILE_SIZE = 256;
    private static final String TILE_URL = "https://tile.openstreetmap.org/{z}/{x}/{y}.png";

    private double centerLat = 34.3740;
    private double centerLon = 108.9100;
    private int zoom = 15;

    private double offsetX = 0;
    private double offsetY = 0;

    private final Map<String, Image> tileCache = new HashMap<>();
    private final ExecutorService tileLoader = Executors.newFixedThreadPool(4);

    private double dragStartX, dragStartY;
    private boolean isDragging = false;

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

    public CanvasMapRenderer(double width, double height) {
        super(width, height);
        setupMouseHandlers();
        render();
    }

    private void setupMouseHandlers() {
        setOnMousePressed(e -> {
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

    setOnMouseReleased(e -> isDragging = false);

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
                System.out.println("[Zoom] " + oldZoom + " -> " + zoom + " | center=(" + centerLat + ", " + centerLon + ")");
                render();
          }
      });
    }

    private void render() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.setFill(Color.web("#f0f0f0"));
        gc.fillRect(0, 0, getWidth(), getHeight());

        int centerTileX = lon2tile(centerLon, zoom);
        int centerTileY = lat2tile(centerLat, zoom);
      System.out.println("[Render] zoom=" + zoom + " | centerTile=(" + centerTileX + ", " + centerTileY + ") | offset=(" + offsetX + ", " + offsetY + ")");

        int tilesX = (int) Math.ceil(getWidth() / TILE_SIZE) + 2;
    int tilesY = (int) Math.ceil(getHeight() / TILE_SIZE) + 2;

        for (int dx = -tilesX / 2; dx <= tilesX / 2; dx++) {
      for (int dy = -tilesY / 2; dy <= tilesY / 2; dy++) {
                int tileX = centerTileX + dx;
                int tileY = centerTileY + dy;
          if (tileX < 0 || tileY < 0 || tileX >= (1 << zoom) || tileY >= (1 << zoom)) {
                 continue;
              }

                double screenX = getWidth() / 2 + dx * TILE_SIZE + offsetX;
             double screenY = getHeight() / 2 + dy * TILE_SIZE + offsetY;

            String key = zoom + "/" + tileX + "/" + tileY;
                Image tile = tileCache.get(key);

                if (tile != null) {
               gc.drawImage(tile, screenX, screenY);
                } else {
                    gc.setFill(Color.web("#e0e0e0"));
                  gc.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
                  loadTileAsync(tileX, tileY, zoom, key);
                }
        }
        }

        renderMarkers(gc);
    }

    private void renderMarkers(GraphicsContext gc) {
        for (BuildingMarker m : markers) {
       double[] screen = latLonToScreen(m.lat, m.lon);
            if (screen == null) continue;

            gc.setFill(m.color);
            gc.fillOval(screen[0] - 8, screen[1] - 8, 16, 16);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(3);
            gc.strokeOval(screen[0] - 8, screen[1] - 8, 16, 16);
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
    private double lon2tileExact(double lon, int zoom) {
        return (lon + 180.0) / 360.0 * (1 << zoom);
    }

    private double lat2tileExact(double lat, int zoom) {
        double latRad = Math.toRadians(lat);
        return (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 << zoom);
    }

    private BuildingMarker findMarkerAt(double x, double y) {
        for (BuildingMarker m : markers) {
            double[] screen = latLonToScreen(m.lat, m.lon);
            if (screen == null) continue;
            double dist = Math.sqrt(Math.pow(x - screen[0], 2) + Math.pow(y - screen[1], 2));
            if (dist <= 12) return m;
        }
        return null;
    }

    private void loadTileAsync(int tileX, int tileY, int z, String key) {
        tileLoader.submit(() -> {
            try {
                String urlStr = TILE_URL
                  .replace("{z}", String.valueOf(z))
            .replace("{x}", String.valueOf(tileX))
                    .replace("{y}", String.valueOf(tileY));

                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7897));
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection(proxy);
              conn.setRequestProperty("User-Agent", "CampusMapSystem/1.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                try (InputStream is = conn.getInputStream()) {
            Image img = new Image(is);
            tileCache.put(key, img);
            tileCache.put(key, img);
            // 只在当前缩放级别匹配时才重绘，避免旧瓦片触发渲染
             if (z == zoom) {
            javafx.application.Platform.runLater(this::render);
                }
             }
            } catch (Exception e) {
            System.err.println("[Tile] 加载失败: " + key + " - " + e.getMessage());
            }
        });
    }

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
    }
}
