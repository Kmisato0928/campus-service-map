package edu.chd.campusmap.service;

import edu.chd.campusmap.dao.PathEdgeDAO;
import edu.chd.campusmap.dao.PathNodeDAO;
import edu.chd.campusmap.model.Building;
import edu.chd.campusmap.model.PathEdge;
import edu.chd.campusmap.model.PathNode;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.*;

public class NavigationService {

    private final PathNodeDAO nodeDAO = new PathNodeDAO();
    private final PathEdgeDAO edgeDAO = new PathEdgeDAO();
    private final MapService mapService = new MapService();

    // 主图：来自 OSM 数据
    private Graph<Integer, DefaultWeightedEdge> graph;
    private Map<Integer, PathNode> nodeMap;
    private boolean osmLoaded = false;

    // 回退图：从建筑坐标生成
    private Graph<Integer, DefaultWeightedEdge> fallbackGraph;
    private Map<Integer, PathNode> fallbackNodeMap;
    private boolean fallbackReady = false;

    public NavigationService() {
        reloadGraph();
    }

    public void reloadGraph() {
        List<PathNode> nodes = nodeDAO.findAll();
        List<PathEdge> edges = edgeDAO.findAll();

        nodeMap = new HashMap<>();
        graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        for (PathNode n : nodes) {
            graph.addVertex(n.getId());
            nodeMap.put(n.getId(), n);
        }

        int edgeCount = 0;
        for (PathEdge e : edges) {
            if (!graph.containsVertex(e.getStartNodeId()) || !graph.containsVertex(e.getEndNodeId())) {
                continue;
            }
            DefaultWeightedEdge edge = graph.addEdge(e.getStartNodeId(), e.getEndNodeId());
            if (edge != null) {
                graph.setEdgeWeight(edge, e.getDistanceMeters());
                edgeCount++;
            }
        }

        osmLoaded = !nodes.isEmpty() && edgeCount > 0;
        if (osmLoaded) {
            System.out.println("[Nav] OSM 路网加载完成: " + nodeMap.size() + " 节点, " + edgeCount + " 边");
        } else {
            System.out.println("[Nav] OSM 路网未加载，准备建筑坐标回退图");
            buildFallbackGraph();
        }
    }

    /**
     * 从建筑坐标构建回退路网图。
     * 每栋建筑作为一个节点，连接附近建筑形成通行网络。
     */
    private void buildFallbackGraph() {
        List<Building> buildings = mapService.getAllBuildings();
        if (buildings.isEmpty()) return;

        fallbackNodeMap = new HashMap<>();
        fallbackGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        // 为每栋建筑创建节点，用负数 ID 避免与可能的数据库 ID 冲突
        for (Building b : buildings) {
            PathNode node = new PathNode(b.getLatitude(), b.getLongitude(), b.getName(), b.getId());
            node.setId(-b.getId());
            fallbackGraph.addVertex(node.getId());
            fallbackNodeMap.put(node.getId(), node);
        }

        // 连接附近的建筑：每个建筑连到最近的 4 个建筑（如果距离 < 500 米）
        List<Integer> ids = new ArrayList<>(fallbackNodeMap.keySet());
        int edgeCount = 0;
        double maxDist = 500;

        for (int i = 0; i < ids.size(); i++) {
            PathNode ni = fallbackNodeMap.get(ids.get(i));
            // 计算到其他建筑的距离，取最近的 4 个
            PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));

            for (int j = 0; j < ids.size(); j++) {
                if (i == j) continue;
                PathNode nj = fallbackNodeMap.get(ids.get(j));
                double dist = haversine(ni.getLat(), ni.getLon(), nj.getLat(), nj.getLon());
                if (dist <= maxDist) {
                    pq.offer(new int[]{(int) dist, ids.get(j)});
                }
            }

            int connections = 0;
            while (!pq.isEmpty() && connections < 4) {
                int[] entry = pq.poll();
                int neighborId = entry[1];
                double dist = entry[0];
                DefaultWeightedEdge edge = fallbackGraph.addEdge(ids.get(i), neighborId);
                if (edge != null) {
                    fallbackGraph.setEdgeWeight(edge, dist);
                    edgeCount++;
                }
                connections++;
            }
        }

        fallbackReady = edgeCount > 0;
        System.out.println("[Nav] 回退图加载完成: " + fallbackNodeMap.size() + " 节点, " + edgeCount + " 边");
    }

    public boolean isLoaded() {
        return osmLoaded || fallbackReady;
    }

    public List<PathNode> findPath(int fromBuildingId, int toBuildingId) {
        Building fromB = mapService.getBuildingById(fromBuildingId);
        Building toB = mapService.getBuildingById(toBuildingId);
        if (fromB == null || toB == null) return Collections.emptyList();

        // 优先使用 OSM 路网
        if (osmLoaded) {
            PathNode fromNode = nodeDAO.findNearest(fromB.getLatitude(), fromB.getLongitude());
            PathNode toNode = nodeDAO.findNearest(toB.getLatitude(), toB.getLongitude());
            if (fromNode != null && toNode != null) {
                List<PathNode> result = findPathInGraph(graph, nodeMap, fromNode.getId(), toNode.getId());
                if (!result.isEmpty()) return result;
            }
        }

        // 使用建筑坐标回退图
        if (fallbackReady) {
            int fromId = -fromBuildingId;
            int toId = -toBuildingId;
            List<PathNode> result = findPathInGraph(fallbackGraph, fallbackNodeMap, fromId, toId);
            if (!result.isEmpty()) {
                System.out.println("[Nav] 使用回退图路径: " + fromB.getName() + " → " + toB.getName());
                return result;
            }
        }

        return Collections.emptyList();
    }

    private List<PathNode> findPathInGraph(Graph<Integer, DefaultWeightedEdge> g,
                                            Map<Integer, PathNode> nMap,
                                            int fromId, int toId) {
        if (!g.containsVertex(fromId) || !g.containsVertex(toId)) {
            return Collections.emptyList();
        }
        if (fromId == toId) {
            PathNode node = nMap.get(fromId);
            return node != null ? Collections.singletonList(node) : Collections.emptyList();
        }

        DijkstraShortestPath<Integer, DefaultWeightedEdge> dijkstra = new DijkstraShortestPath<>(g);
        GraphPath<Integer, DefaultWeightedEdge> path = dijkstra.getPath(fromId, toId);

        if (path == null) return Collections.emptyList();

        List<PathNode> result = new ArrayList<>();
        for (Integer nodeId : path.getVertexList()) {
            PathNode node = nMap.get(nodeId);
            if (node != null) result.add(node);
        }
        return result;
    }

    public double calculateTotalDistance(List<PathNode> path) {
        double total = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            PathNode a = path.get(i);
            PathNode b = path.get(i + 1);
            total += haversine(a.getLat(), a.getLon(), b.getLat(), b.getLon());
        }
        return total;
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public Map<Integer, PathNode> getNodeMap() {
        return osmLoaded ? nodeMap : fallbackNodeMap;
    }
}
