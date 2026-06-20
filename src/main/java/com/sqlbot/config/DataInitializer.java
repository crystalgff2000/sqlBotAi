package com.sqlbot.config;

import com.sqlbot.entity.*;
import com.sqlbot.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private DataAssetRepository dataAssetRepository;

    @Autowired
    private KnowledgeGraphNodeRepository nodeRepository;

    @Autowired
    private KnowledgeGraphEdgeRepository edgeRepository;

    @Override
    public void run(String... args) {
        initConcepts();
        initEntities();
        initDataAssets();
        initKnowledgeGraph();
    }

    private void initConcepts() {
        if (conceptRepository.count() == 0) {
            Concept c1 = new Concept();
            c1.setName("\u6570\u636e\u4ed3\u5e93");
            c1.setCategory("\u6570\u636e\u6982\u5ff5");
            c1.setDescription("\u7528\u4e8e\u5b58\u50a8\u548c\u7ba1\u7406\u4f01\u4e1a\u6570\u636e\u7684\u96c6\u4e2d\u5f0f\u5b58\u50a8\u7cfb\u7edf");
            c1.setDefinition("\u6570\u636e\u4ed3\u5e93\u662f\u4e00\u4e2a\u9762\u5411\u4e3b\u9898\u7684\u3001\u96c6\u6210\u7684\u3001\u76f8\u5bf9\u7a33\u5b9a\u7684\u3001\u53cd\u6620\u5386\u53f2\u53d8\u5316\u7684\u6570\u636e\u96c6\u5408\uff0c\u7528\u4e8e\u652f\u6301\u7ba1\u7406\u51b3\u7b56\u3002");
            conceptRepository.save(c1);

            Concept c2 = new Concept();
            c2.setName("\u6570\u636e\u6e56");
            c2.setCategory("\u6570\u636e\u6982\u5ff5");
            c2.setDescription("\u5b58\u50a8\u539f\u59cb\u6570\u636e\u7684\u5b58\u50a8\u5e93\uff0c\u652f\u6301\u7ed3\u6784\u5316\u3001\u534a\u7ed3\u6784\u5316\u548c\u975e\u7ed3\u6784\u5316\u6570\u636e");
            c2.setDefinition("\u6570\u636e\u6e56\u662f\u4e00\u4e2a\u5b58\u50a8\u5e93\uff0c\u53ef\u4ee5\u4ee5\u539f\u751f\u683c\u5f0f\u5b58\u50a8\u5927\u91cf\u539f\u59cb\u6570\u636e\uff0c\u76f4\u5230\u9700\u8981\u4f7f\u7528\u4e3a\u6b62\u3002");
            conceptRepository.save(c2);

            Concept c3 = new Concept();
            c3.setName("\u4e3b\u6570\u636e\u7ba1\u7406");
            c3.setCategory("\u4e1a\u52a1\u6982\u5ff5");
            c3.setDescription("\u7ba1\u7406\u4f01\u4e1a\u6838\u5fc3\u4e1a\u52a1\u5b9e\u4f53\u7684\u6570\u636e\uff0c\u5982\u5ba2\u6237\u3001\u4ea7\u54c1\u3001\u4f9b\u5e94\u5546\u7b49");
            c3.setDefinition("\u4e3b\u6570\u636e\u7ba1\u7406\u662f\u4e00\u79cd\u7efc\u5408\u65b9\u6cd5\uff0c\u7528\u4e8e\u7ba1\u7406\u4f01\u4e1a\u5173\u952e\u4e1a\u52a1\u6570\u636e\u7684\u4e00\u81f4\u6027\u548c\u51c6\u786e\u6027\u3002");
            conceptRepository.save(c3);

            Concept c4 = new Concept();
            c4.setName("ETL");
            c4.setCategory("\u6280\u672f\u6982\u5ff5");
            c4.setDescription("\u63d0\u53d6\u3001\u8f6c\u6362\u3001\u52a0\u8f7d\uff0c\u6570\u636e\u96c6\u6210\u8fc7\u7a0b\u7684\u6838\u5fc3\u6280\u672f");
            c4.setDefinition("ETL\u662f\u5c06\u6570\u636e\u4ece\u6e90\u7cfb\u7edf\u63d0\u53d6\u3001\u8fdb\u884c\u8f6c\u6362\u5904\u7406\u5e76\u52a0\u8f7d\u5230\u76ee\u6807\u7cfb\u7edf\u7684\u8fc7\u7a0b\u3002");
            conceptRepository.save(c4);

            Concept c5 = new Concept();
            c5.setName("\u6570\u636e\u6cbb\u7406");
            c5.setCategory("\u4e1a\u52a1\u6982\u5ff5");
            c5.setDescription("\u786e\u4fdd\u6570\u636e\u8d28\u91cf\u3001\u5b89\u5168\u6027\u548c\u5408\u89c4\u6027\u7684\u7ba1\u7406\u6846\u67b6");
            c5.setDefinition("\u6570\u636e\u6cbb\u7406\u662f\u5bf9\u6570\u636e\u8d44\u4ea7\u7684\u7ba1\u7406\u548c\u76d1\u7763\uff0c\u786e\u4fdd\u6570\u636e\u7684\u53ef\u7528\u6027\u3001\u5b8c\u6574\u6027\u3001\u5b89\u5168\u6027\u548c\u5408\u89c4\u6027\u3002");
            conceptRepository.save(c5);

            System.out.println("\u521d\u59cb\u5316\u6982\u5ff5\u6570\u636e: " + conceptRepository.count() + " \u6761");
        }
    }

    private void initEntities() {
        if (entityRepository.count() == 0) {
            EntityItem e1 = new EntityItem();
            e1.setName("customers");
            e1.setType("\u6570\u636e\u5e93\u8868");
            e1.setDescription("\u5ba2\u6237\u4fe1\u606f\u4e3b\u8868");
            e1.setAttributes("id, name, email, phone, address, created_at");
            e1.setDataSource("MySQL");
            entityRepository.save(e1);

            EntityItem e2 = new EntityItem();
            e2.setName("orders");
            e2.setType("\u6570\u636e\u5e93\u8868");
            e2.setDescription("\u8ba2\u5355\u4fe1\u606f\u8868");
            e2.setAttributes("id, customer_id, amount, status, created_at");
            e2.setDataSource("MySQL");
            entityRepository.save(e2);

            EntityItem e3 = new EntityItem();
            e3.setName("User API");
            e3.setType("API\u63a5\u53e3");
            e3.setDescription("\u7528\u6237\u4fe1\u606f\u67e5\u8be2\u63a5\u53e3");
            e3.setAttributes("GET /api/users, POST /api/users");
            e3.setDataSource("REST API");
            entityRepository.save(e3);

            EntityItem e4 = new EntityItem();
            e4.setName("sales_report.csv");
            e4.setType("\u6587\u4ef6");
            e4.setDescription("\u9500\u552e\u62a5\u8868\u6570\u636e\u6587\u4ef6");
            e4.setAttributes("date, product, quantity, revenue");
            e4.setDataSource("File System");
            entityRepository.save(e4);

            EntityItem e5 = new EntityItem();
            e5.setName("Data Sync Service");
            e5.setType("\u670d\u52a1");
            e5.setDescription("\u6570\u636e\u540c\u6b65\u5fae\u670d\u52a1");
            e5.setAttributes("source, target, schedule, status");
            e5.setDataSource("Microservice");
            entityRepository.save(e5);

            System.out.println("\u521d\u59cb\u5316\u5b9e\u4f53\u6570\u636e: " + entityRepository.count() + " \u6761");
        }
    }

    private void initDataAssets() {
        if (dataAssetRepository.count() == 0) {
            DataAsset a1 = new DataAsset();
            a1.setName("production_db");
            a1.setType("\u6570\u636e\u5e93");
            a1.setDescription("\u751f\u4ea7\u73af\u5883\u4e3b\u6570\u636e\u5e93");
            a1.setFormat("SQL");
            a1.setSize(1073741824L); // 1GB
            a1.setOwner("DBA Team");
            a1.setAccessLevel("Restricted");
            dataAssetRepository.save(a1);

            DataAsset a2 = new DataAsset();
            a2.setName("customer_data.csv");
            a2.setType("\u6587\u4ef6");
            a2.setDescription("\u5ba2\u6237\u4fe1\u606f\u5bfc\u51fa\u6587\u4ef6");
            a2.setFormat("CSV");
            a2.setSize(104857600L); // 100MB
            a2.setOwner("Analytics Team");
            a2.setAccessLevel("Internal");
            dataAssetRepository.save(a2);

            DataAsset a3 = new DataAsset();
            a3.setName("sales_api");
            a3.setType("API");
            a3.setDescription("\u9500\u552e\u6570\u636eAPI\u670d\u52a1");
            a3.setFormat("JSON");
            a3.setSize(0L);
            a3.setOwner("Engineering Team");
            a3.setAccessLevel("Public");
            dataAssetRepository.save(a3);

            DataAsset a4 = new DataAsset();
            a4.setName("product_catalog.json");
            a4.setType("\u6587\u4ef6");
            a4.setDescription("\u4ea7\u54c1\u76ee\u5f55\u6570\u636e");
            a4.setFormat("JSON");
            a4.setSize(52428800L); // 50MB
            a4.setOwner("Product Team");
            a4.setAccessLevel("Public");
            dataAssetRepository.save(a4);

            DataAsset a5 = new DataAsset();
            a5.setName("inventory_report.parquet");
            a5.setType("\u6587\u4ef6");
            a5.setDescription("\u5e93\u5b58\u62a5\u8868\uff08Parquet\u683c\u5f0f\uff09");
            a5.setFormat("Parquet");
            a5.setSize(26214400L); // 25MB
            a5.setOwner("Operations Team");
            a5.setAccessLevel("Internal");
            dataAssetRepository.save(a5);

            System.out.println("\u521d\u59cb\u5316\u6570\u636e\u8d44\u4ea7: " + dataAssetRepository.count() + " \u6761");
        }
    }

    private void initKnowledgeGraph() {
        if (nodeRepository.count() == 0) {
            // \u521b\u5efa\u793a\u4f8b\u8282\u70b9
            String[] nodeNames = {"\u6570\u636e\u5e93", "\u6570\u636e\u8868", "\u5b57\u6bb5", "\u7d22\u5f15", "\u5173\u7cfb", "\u89c6\u56fe", "\u5b58\u50a8\u8fc7\u7a0b"};
            String[] categories = {"\u7cfb\u7edf", "\u5bf9\u8c61", "\u5c5e\u6027", "\u7ed3\u6784", "\u5173\u8054", "\u5bf9\u8c61", "\u903b\u8f91"};

            for (int i = 0; i < nodeNames.length; i++) {
                KnowledgeGraphNode node = new KnowledgeGraphNode();
                node.setNodeId("node_" + i);
                node.setName(nodeNames[i]);
                node.setType("\u57fa\u7840");
                node.setCategory(categories[i]);
                node.setProperties("{}");
                nodeRepository.save(node);
            }

            // \u521b\u5efa\u793a\u4f8b\u5173\u7cfb
            String[][] relations = {
                {"node_0", "node_1", "\u5305\u542b"},
                {"node_1", "node_2", "\u62e5\u6709"},
                {"node_1", "node_3", "\u4f7f\u7528"},
                {"node_1", "node_4", "\u5173\u8054"},
                {"node_0", "node_5", "\u652f\u6301"},
                {"node_0", "node_6", "\u6267\u884c"}
            };

            for (String[] rel : relations) {
                KnowledgeGraphEdge edge = new KnowledgeGraphEdge();
                edge.setSourceNodeId(rel[0]);
                edge.setTargetNodeId(rel[1]);
                edge.setRelation(rel[2]);
                edge.setWeight(1.0);
                edge.setProperties("{}");
                edgeRepository.save(edge);
            }

            System.out.println("\u521d\u59cb\u5316\u77e5\u8bc6\u56fe\u8c31: " + nodeRepository.count() + " \u8282\u70b9, " + edgeRepository.count() + " \u5173\u7cfb");
        }
    }
}
