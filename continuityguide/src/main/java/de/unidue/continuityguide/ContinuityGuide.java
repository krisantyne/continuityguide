package de.unidue.continuityguide;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.MoreLikeThisQueryBuilder.Item;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import de.unidue.continuityguide.Maker.Variable;



/**
 * Indexing, classification and automatic evaluation for Continuity Guide
 */
public class ContinuityGuide {

	static TransportClient client;
	
	static String path = "";
	static String test = "";
	
	
	
	/**
	 * Initialization creates the TransportClient for communicating with Elasticsearch and
	 * reads the properties file
	 */
	public ContinuityGuide() {
		Settings settings = Settings.builder()
				.put("client.transport.sniff", true).build();
		try {
			client = new PreBuiltTransportClient(settings)
					.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		InputStream inputStream = null;
		
		try {
			Properties prop = new Properties();
			String propFileName = "continuityguide.properties";
 
			inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
 
			if (inputStream != null) {
				prop.load(inputStream);
			} else {
				throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
			}
			// get the property value and print it out
			path = prop.getProperty("path");
			test = path + "test";
 
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * Automatic evaluation, prints results on console
	 */
	public void evaluate() {
		
		float sumPrecicison = 0;
		float sumRecall = 0;
		File[] xmldocs;


		xmldocs = new File(test).listFiles();

		ArrayList<File> fileList = new ArrayList<File>(Arrays.asList(xmldocs));
		
		for(int i=0; i < fileList.size(); i++) {
			
			String filepath = fileList.get(i).getPath();
			Variable v = Maker.parse(filepath);

			String realCategoryUnprocessed = v.finalcat;
			String realCategory = realCategoryUnprocessed.replace("/ ", "");

			List<String> suggestedCategories;
			
			suggestedCategories = classify(filepath);

			float precision = calcPrecision(realCategory, suggestedCategories);
			float recall = calcRecall(realCategory, suggestedCategories);

			sumPrecicison = sumPrecicison + precision;
			sumRecall = sumRecall + recall;

			System.out.println(i + " " + v.id + " " + v.label + " | " + "Real: " + realCategory
					+ " Suggested: " + suggestedCategories + " Precision: " + precision + " Recall: " + recall);
		}

		float avgPrecision = sumPrecicison / fileList.size();
		float avgRecall = sumRecall / fileList.size();

		float fbalance = 2;
		float fmeasure = (float) ((float) ((1 + Math.pow(fbalance, 2)) * avgPrecision * avgRecall) / (Math.pow(fbalance, 2) * avgPrecision + avgRecall));

		System.out.println("Continuityguide: Precision: " + avgPrecision + " Recall: " + avgRecall + " F: " + fmeasure);
	}
 	
	
	/**
	 * Calculates Precision for one study
	 * @param real Real category
	 * @param suggested Suggested categories
	 * @return Precision
	 */
	private float calcPrecision(String real, List<String> suggested) {

		int retrieved = suggested.size();
		int relevantretrieved = 0;


		if (suggested.contains(real)) {
			relevantretrieved = 1;
		}

		float precision = 0;
		
		if (retrieved != 0) {
			precision = (float) relevantretrieved / retrieved;
		}
		return precision;

	}


	/**
	 * Calculates Recall for one study
	 * @param real Real category
	 * @param suggested Suggested categories
	 * @return Recall
	 */
	private float calcRecall(String real, List<String> suggested) {

		int relevant = 1;
		int relevantretrieved = 0;


		if (suggested.contains(real)) {
			relevantretrieved = 1;
		}
		

		float recall = (float) relevantretrieved / relevant;
		return recall;
	}
	
	
	/**
	 * Creates continuityguide index in Elasticsearch or resets it if it already exists, 
	 * creates index mappings, fills index with data from the megadoc files
	 */
	public void makeIndex() {
		
		boolean exists = client.admin().indices()
			    .prepareExists("continuityguide")
			    .execute().actionGet().isExists();
		
		if (exists == true) {
			DeleteIndexRequest request = new DeleteIndexRequest("continuityguide");
		    try {
		        DeleteIndexResponse response = client.admin().indices().delete(request).actionGet();
		        if (!response.isAcknowledged()) {
		            throw new Exception("Failed to delete index " + "continuityguide");
		        }
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		}
		
		client.admin().indices().prepareCreate("continuityguide")
		.setSettings(Settings.builder()             
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 0))
		.get();
		
		
		try {

			XContentBuilder indexMappings = XContentFactory.jsonBuilder().
					startObject().
					startObject("megadoc").
					startObject("properties").
					startObject("ids").
					field("type", "text").field("index", "false").
					endObject().
					startObject("studynos").
					field("type", "text").field("term_vector", "yes").
					endObject().
					startObject("varinos").
					field("type", "text").field("term_vector", "yes").
					endObject().
					startObject("labels").
					field("type", "text").field("analyzer", "english").field("term_vector", "yes").
					endObject().
					startObject("qtexts").
					field("type", "text").field("analyzer", "english").field("term_vector", "yes").
					endObject().
					startObject("category").
					field("type", "keyword").
					endObject().
					endObject().
					endObject().
					endObject();


			client.admin().indices().preparePutMapping("continuityguide").setType("megadoc").setSource(indexMappings).get();


			File[] fileList = new File(path + "/mega").listFiles();

			for(int i=0; i < fileList.length; i++) {
				String filepath = fileList[i].getPath();
				String filename = fileList[i].getName();
				filename = filename.substring( 0, filename.indexOf( ".xml" ) );

				Map<String, Object> jsonDocument = parseMegadoc(filepath);
				jsonDocument.put("category", filename);



				IndexResponse response = client.prepareIndex("continuityguide", "megadoc").setSource(jsonDocument)
						.get();

				String _index = response.getIndex();
				String _type = response.getType();
				String _id = response.getId();
				long _version = response.getVersion();
				RestStatus status = response.status();

				System.out.println(_index + " " + _type + " " + _id + " " + _version + " " + status);

			}


		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Megadocument classifier implementation using MoreLikeThis function from Elasticsearch
	 * @param inFile
	 * @return
	 */
	public List<String> classify(String inFile) {
		XContentBuilder newDoc = buildXContent(Maker.parse(inFile));

		if (newDoc != null) {
		Item item1 = new Item("continuityguide", "megadoc", newDoc);
		Item[] items1 = {item1};

		try {
			System.out.println(newDoc.string());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		SearchResponse response = client.prepareSearch("continuityguide")
				.setQuery(QueryBuilders
						.moreLikeThisQuery(items1)
						.minTermFreq(1)
						.maxQueryTerms(10)
						.minDocFreq(1))
				.setTypes("megadoc")
				.get();


		List<String> suggestions = new ArrayList<String>();
		
		if (response.getHits().totalHits() != 0) {
			double topscore = response.getHits().getAt(0).getScore();

			for (int i=0; i<response.getHits().getTotalHits(); i++) {
				double score = response.getHits().getAt(i).getScore();
				if (score*1.3 < topscore) break;
				Map<String, Object> field = response.getHits().getAt(i).sourceAsMap();
				String category = (String) field.get("category");
				suggestions.add(category);
				if (i==9) break;
			}
		}
		
		return suggestions;
		} else {
			return null;
		}
	}
	
	
	/**
	 * Makes JSON from a megadocument file
	 * @param inFile The megadocument file
	 * @return JSON formatted content
	 */
	private Map<String, Object> parseMegadoc(String inFile) {
		org.jdom2.Document doc = new org.jdom2.Document();
		try {
			doc = new SAXBuilder().build(inFile);
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}
		Element root = doc.getRootElement();
		String ids = root.getChildText("ids");
		String studynos = root.getChildText("studynos");
		String varinos = root.getChildText("varinos");
		String labels = root.getChildText("labels");
		String qtexts = root.getChildText("qtexts");

		Map<String, Object> jsonDocument = new HashMap<String, Object>();

		jsonDocument.put("ids", ids);
		jsonDocument.put("studynos", studynos);
		jsonDocument.put("varinos", varinos);
		jsonDocument.put("labels", labels);
		jsonDocument.put("qtexts", qtexts);
		

		return jsonDocument;
	}
	
	/**
	 * Transforms Variable object into JSON format for Elasticsearch
	 * @param v Input Variable
	 * @return XContentBuilder with JSON of the Variable
	 */
	private XContentBuilder buildXContent(Variable v) {
		XContentBuilder newDoc = null;
		if (v != null) {
		try {
			newDoc = XContentFactory.jsonBuilder()
					.startObject()
						.field("category", "")
						.field("ids", v.id)
						.field("studynos", v.studyno)
						.field("varinos", v.varino)
						.field("labels", v.label)
						.field("qtexts", v.qtext)
					.endObject();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		}
		return newDoc;
		
	}
}
