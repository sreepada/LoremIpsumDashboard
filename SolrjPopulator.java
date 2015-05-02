import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile;
import org.apache.nutch.util.NutchConfiguration;
import org.apache.nutch.crawl.Inlinks;
import org.apache.nutch.crawl.Inlink;

import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.util.*;
import java.io.*;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

class pageRankUrl {
    private String inLink;
    private String baseUrl;
    private int weight = 1;
    public pageRankUrl (String inLink, String baseUrl) {
        this.inLink = inLink;
        this.baseUrl = baseUrl;
    }
    public String getInLink() {
        return inLink;
    }
    public String getBaseUrl() {
        return baseUrl;
    }
    public int getWeight() {
        return weight;
    }
}

class linkNode {
    private String linkA;
    private String linkB;
    private double weight;
    public linkNode (String linkA, String linkB, double weight) {
        this.linkA = linkA;
        this.linkB = linkB;
        this.weight = weight;
    }
    public String getLinkA() {
        return linkA;
    }
    public String getLinkB() {
        return linkB;
    }
    public double getWeight() {
        return weight;
    }
    public void setWeight(double weight) {
        this.weight = weight;
    }
}

public class SolrjPopulator {
    public static void main(String[] args) throws IOException, SolrServerException {

        if(args.length < 3) {
            System.out.println("Usage:");
            System.out.println("java SolrjPopulator <segment_path> <linkdb_data_path> <content_dump_dir>");
            System.exit(0);
        }

        LocationExtractor extractor = null;

        try
        {
            extractor = new LocationExtractor("IndexDirectory");
        }
        catch(Exception e)
        {
            System.out.println("clavin directory not found. Please download index from git repo");
        }

        Map<String, String> urlDidMap = new HashMap<String, String>();
        ArrayList<pageRankUrl> graph = new ArrayList<pageRankUrl>();
        ArrayList<linkNode> linkGraph = new ArrayList<linkNode>();
        try {
            Configuration confForReader = NutchConfiguration.create();
            FileSystem fs = FileSystem.get(confForReader);
            Path segmentFile = new Path(args[0]);
            SequenceFile.Reader segmentReader = new SequenceFile.Reader(fs, segmentFile, confForReader);
            Text segmentKey = new Text();
            while (segmentReader.next(segmentKey)) {
                urlDidMap.put(segmentKey.toString(), "dummyDid");
            }

            Path linkFile = new Path(args[1]);
            SequenceFile.Reader linkReader = new SequenceFile.Reader(fs, linkFile, confForReader);
            Inlinks segmentInLink = new Inlinks();
            while (linkReader.next(segmentKey, segmentInLink)) {
                Iterator<Inlink> currLink = segmentInLink.iterator();
                while(currLink.hasNext()) {
                    String somehting = (currLink.next().toString().split(" ")[1]).trim();
                    graph.add(new pageRankUrl(somehting, segmentKey.toString()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String urlString = "http://localhost:8983/solr"; 
        SolrServer solr = new HttpSolrServer(urlString);

        File dir = new File(args[2]);
        FileFilter fileFilter = new WildcardFileFilter("*.html*");
        File[] files = dir.listFiles(fileFilter);
        String currDocID;
        for (int i = 0; i < files.length; i++)
        {
            currDocID = "html-" + i;
            for(String key : urlDidMap.keySet()) {
                String urlLast = key.substring(key.lastIndexOf("/") + 1);
                String fileBasename = files[i].toString().substring(files[i].toString().lastIndexOf("/") + 1);
                if (urlLast.equals(fileBasename) || fileBasename.equals(urlLast + ".html")
                        || fileBasename.equals(urlLast + ".html")) {
                    urlDidMap.put(key, currDocID); 
                        }
            }
            ContentStreamUpdateRequest up = new ContentStreamUpdateRequest("/update/extract");

            up.addFile(new File(files[i].toString()), "text/html");

            Document doc = Jsoup.parse(files[i], "UTF-8", "http://example.com/");
            Elements elements;
            try {
                elements = doc.body().getElementsByClass("descriptive_row_table");
            }
            catch(NullPointerException e) {
                System.out.println("ullaasdfasdf");
                continue;
            }

            try
            {
                if(extractor != null)
                {
                    List<GeoData>  location_list = extractor.getLocationInfoFromFile(files[i]);
                    String location = "";
                    for(GeoData gd: location_list)
                    {
                        location += gd.getGeoName()+ "==" + Double.toString(gd.getLatitude()) + "==" +  Double.toString(gd.getLongitude()) + ",";
                    }
                    up.setParam("literal.clavin_location_s", location.substring(0, location.length() - 1));
                }
            }
            catch(Exception e)
            {
                System.out.println("");

            }
            up.setParam("literal.id", currDocID);
            up.setParam("id", currDocID);
            up.setParam("literal.title", doc.title());
            up.setParam("literal.page_rank_d", "0");
            Double bboxValues[] = new Double[4];

            for(Element element: elements)
            {
                String text = element.text();
                String[] key_val_pair = text.split(":", 2);
                String key = key_val_pair[0].toLowerCase().replace(" ", "_");
                key = key + "_s";
                String value = key_val_pair[1].replace(" > ", ",");
                if(key.endsWith("_coordinate"))
                {
                    value = value.replaceAll("^[ ()]+", "").replaceAll("[ ()]+$", "");
                    value = value.split(" ")[0];
                }
                if (key.contains("northernmost_l")) {
                    bboxValues[2] = Double.parseDouble(value.trim().split(" ")[0]);
                }
                else if (key.contains("southernmost_l"))
                    bboxValues[3] = Double.parseDouble(value.trim().split(" ")[0]);
                else if (key.contains("westernmost_l"))
                    bboxValues[0] = Double.parseDouble(value.trim().split(" ")[0]);
                else if (key.contains("easternmost_l"))
                    bboxValues[1] = Double.parseDouble(value.trim().split(" ")[0]);
                else
                    up.setParam("literal." + key, value);

                if(key.equals("date_created_s"))
                {
                    System.out.println("year=" + value.trim().split("-")[0]);
                    up.setParam("literal.year_created_s", value.trim().split("-")[0]);
                }
            }
            if (bboxValues[0] != null) {
                up.setParam("literal.bbox", "ENVELOPE(" + bboxValues[0] + ", "
                        + bboxValues[1] + ", " + bboxValues[2] + ", " + bboxValues[3] + ")");
            }
            up.setParam("fmap.content", "attr_content");
            up.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);
            try {
                solr.request(up);
                System.out.println(currDocID + " " + files[i]);
            } catch (Exception e) {
                System.out.println("Exception while trying to index " + currDocID + " " + files[i]);
                e.printStackTrace();
            }
        } 

        for (pageRankUrl temp : graph) {
            if (urlDidMap.containsKey(temp.getInLink()) && urlDidMap.containsKey(temp.getBaseUrl())
                    && !urlDidMap.get(temp.getInLink()).equals("dummyDid")
                    && !urlDidMap.get(temp.getBaseUrl()).equals("dummyDid")) 
            {
                linkGraph.add(new linkNode(
                            urlDidMap.get(temp.getInLink()),
                            urlDidMap.get(temp.getBaseUrl()),
                            (double) temp.getWeight()
                            )
                        );
            }
        }

        SolrQuery myQuery = new SolrQuery("*:*");
        myQuery.setRows(13000);
        QueryResponse rsp1 = solr.query(myQuery);
        SolrDocumentList results = rsp1.getResults();
        SolrDocumentList results2 = rsp1.getResults();
        ArrayList<linkNode> linkGraphNew = new ArrayList<linkNode>();
        for (int k=0; k < results.size(); k++) {
            String docId1 = results.get(k).getFieldValue("id").toString();
            if (results.get(k).getFieldValues("science_keywords_s") != null) {
                String scienceString1 = results.get(k).getFieldValues("science_keywords_s").toString();
                for (int k2=0; k2 < results2.size(); k2++) {
                    String docId2 = results.get(k2).getFieldValue("id").toString();
                    if (results.get(k2).getFieldValues("science_keywords_s") != null && !docId1.equals(docId2)) {
                        String scienceString2 = results.get(k2).getFieldValues("science_keywords_s").toString();
                        Set<String> docSet1 = new HashSet<String>(Arrays.asList(scienceString1.trim().split(",")));
                        Set<String> docSet2 = new HashSet<String>(Arrays.asList(scienceString2.trim().split(",")));
                        int minLength = (docSet1.size() < docSet2.size()) ? docSet1.size() : docSet2.size();
                        docSet1.retainAll(docSet2);
                        if (docSet1.size() >= minLength * 0.5) {
                            int tempI = 0;
                            Boolean a2b = false;
                            Boolean b2a = false;
                            for (linkNode temp : linkGraph) {
                                if (temp.getLinkA().equals(docId1) && temp.getLinkB().equals(docId2)) {
                                    double tempWeight = 1.5;
                                    linkGraph.set(tempI, new linkNode(docId1, docId2, tempWeight));
                                    a2b = true;
                                }
                                if (temp.getLinkA().equals(docId2) && temp.getLinkB().equals(docId1)) {
                                    double tempWeight = 1.5;
                                    linkGraph.set(tempI, new linkNode(docId2, docId1, tempWeight));
                                    b2a = true;
                                }
                                tempI++;
                            }
                            if (!a2b) {
                                linkGraphNew.add(new linkNode(docId1, docId2, 0.5));
                            }
                            if (!b2a) {
                                linkGraphNew.add(new linkNode(docId2, docId1, 0.5));
                            }
                        }
                    } 
                }
            } 
        }

        linkGraph.addAll(linkGraphNew);

        PrintWriter writer = new PrintWriter("temp.txt", "UTF-8");
        for (linkNode temp : linkGraph) {
            writer.println(temp.getLinkA() + " " + temp.getLinkB() + " " + temp.getWeight());
        }
        writer.close();

        try{
            ProcessBuilder pb = new ProcessBuilder("python","pagerank.py");
            Process p = pb.start();

            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while((line = in.readLine()) != null) {
                String docId = line.split(" ")[0];
                String newValue = line.split(" ")[1];
                SolrInputDocument solr_doc = new SolrInputDocument();
                solr_doc.addField("id" , docId); 
                Map<String, Object> fieldModifier = new HashMap<>(1);
                fieldModifier.put("set", newValue);
                solr_doc.addField("page_rank_d", fieldModifier);
                solr.add(solr_doc);
                solr.commit(); 
            } 
        } catch(Exception e) {
            System.out.println(e);
        }
        QueryResponse rsp = solr.query(new SolrQuery("*:*"));
        results = rsp.getResults();
        System.out.println("-----------------------------------------------------------------------------------------------------------------");
        for (int k=0; k < results.size(); k++) {
            System.out.println(results.get(k).getFieldNames());
        }
    }
}
