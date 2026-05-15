package edu.chd.campusmap.util;

import edu.chd.campusmap.dao.PathEdgeDAO;
import edu.chd.campusmap.dao.PathEdgeDAO;
import edu.chd.campusmap.dao.PathNodeDAO;
import edu.chd.campusmap.model.PathEdge;
import edu.chd.campusmap.model.PathNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class OverpassImporter {

    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
    private static final String BBOX = "34.355,108.885,34.390,108.935";
    private static final String QUERY = "[out:json][timeout:60];(way[\"highway\"](" + BBOX + "););(._;>;);out body;";

    private static Runnable onComplete;
    private static Consumer<String> onProgress;

    public static void setOnComplete(Runnable callback) {
        onComplete = callback;
    }

    public static void setOnProgress(Consumer<String> callback) {
        onProgress = callback;
    }

    private static void reportProgress(String msg) {
        javafx.application.Platform.runLater(() -> {
            if (onProgress != null) onProgress.accept(msg);
        });
    }

    public static boolean importIfEmpty() {
        PathNodeDAO nodeDAO = new PathNodeDAO();
        PathEdgeDAO edgeDAO = new PathEdgeDAO();
        long nodeCount = nodeDAO.count();
        long edgeCount = edgeDAO.count();
        if (nodeCount > 100 && edgeCount > 0) {
            System.out.println("[Overpass] 路网数据已存在 (" + nodeCount + " 节点, " + edgeCount + " 边)，跳过导入");
            return false;
        }
        if (nodeCount > 0 || edgeCount > 0) {
            System.out.println("[Overpass] 检测到不完整数据 (" + nodeCount + " 节点, " + edgeCount + " 边)，清空后重新导入");
            reportProgress("检测到不完整数据，正在清空...");
            nodeDAO.deleteAll();
        }
        reportProgress("正在连接 Overpass API...");
        try {
            String json = fetchOverpassData();
            if (json == null || json.isBlank()) {
                reportProgress("OSM 路网下载失败：未获取到数据");
                return false;
            }
            reportProgress("正在解析路网数据...");
            boolean parsed = parseAndImport(json);
            if (!parsed) {
                reportProgress("OSM 路网数据为空，请检查 Overpass 查询范围");
                return false;
            }
            reportProgress("路网导入完成！");
            if (onComplete != null) {
                javafx.application.Platform.runLater(onComplete);
            }
            return true;
        } catch (Exception e) {
            reportProgress("OSM 路网导入失败: " + e.getMessage());
            System.err.println("[Overpass] 导入失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static String fetchOverpassData() throws Exception {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7897));

        URL url = new URL(OVERPASS_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection(proxy);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("User-Agent", "CampusMapSystem/1.0");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setDoOutput(true);

        String body = "data=" + URLEncoder.encode(QUERY, StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new RuntimeException("Overpass API 返回 " + code);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    static boolean parseAndImport(String json) {
        JSONObject root = new JSONObject(json);
        JSONArray elements = root.getJSONArray("elements");

        reportProgress("正在提取道路节点...");
        // 第一遍：提取所有节点
        List<PathNode> nodes = new ArrayList<>();
        if (elements.isEmpty()) {
            reportProgress("API 返回数据为空，未找到道路数据");
            return false;
        }
        List<Long> osmIds = new ArrayList<>();
        Map<Long, Integer> osmToIndex = new HashMap<>();

        for (int i = 0; i < elements.length(); i++) {
            JSONObject el = elements.getJSONObject(i);
            if (!"node".equals(el.getString("type"))) continue;

            long osmId = el.getLong("id");
            double lat = el.getDouble("lat");
            double lon = el.getDouble("lon");
            String name = null;
            if (el.has("tags")) {
                JSONObject tags = el.getJSONObject("tags");
                if (tags.has("name")) name = tags.getString("name");
            }
            nodes.add(new PathNode(lat, lon, name, null));
            osmIds.add(osmId);
            osmToIndex.put(osmId, nodes.size() - 1);
        }

        System.out.println("[Overpass] 解析到 " + nodes.size() + " 个节点");

        if (nodes.isEmpty()) {
            reportProgress("未提取到道路节点，请检查查询范围");
            return false;
        }

        reportProgress("正在保存 " + nodes.size() + " 个节点到数据库...");
        // 批量入库
        PathNodeDAO nodeDAO = new PathNodeDAO();
        nodeDAO.batchInsert(nodes);

        // 构建 OSM ID → 数据库 ID 映射
        Map<Long, Integer> osmToDb = new HashMap<>();
        for (int i = 0; i < nodes.size(); i++) {
            osmToDb.put(osmIds.get(i), nodes.get(i).getId());
        }

        reportProgress("正在构建道路连接...");
        // 第二遍：从道路创建边
        List<PathEdge> edges = new ArrayList<>();
        for (int i = 0; i < elements.length(); i++) {
            JSONObject el = elements.getJSONObject(i);
            if (!"way".equals(el.getString("type"))) continue;

            JSONArray wayNodes = el.getJSONArray("nodes");
            // 跳过重复节点少于2的道路
            if (wayNodes.length() < 2) continue;

            String roadName = null;
            if (el.has("tags")) {
                JSONObject tags = el.getJSONObject("tags");
                if (tags.has("name")) roadName = tags.getString("name");
            }

            for (int j = 0; j < wayNodes.length() - 1; j++) {
                long fromOsm = wayNodes.getLong(j);
                long toOsm = wayNodes.getLong(j + 1);
                Integer fromDb = osmToDb.get(fromOsm);
                Integer toDb = osmToDb.get(toOsm);
                if (fromDb == null || toDb == null) continue;

                int idxFrom = osmToIndex.get(fromOsm);
                int idxTo = osmToIndex.get(toOsm);
                PathNode fn = nodes.get(idxFrom);
                PathNode tn = nodes.get(idxTo);
                double dist = haversine(fn.getLat(), fn.getLon(), tn.getLat(), tn.getLon());

                edges.add(new PathEdge(fromDb, toDb, dist, roadName));
            }
        }

        System.out.println("[Overpass] 解析到 " + edges.size() + " 条路径段");

        reportProgress("正在保存 " + edges.size() + " 条道路连接...");
        PathEdgeDAO edgeDAO = new PathEdgeDAO();
        edgeDAO.batchInsert(edges);
        return true;
    }

    static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
