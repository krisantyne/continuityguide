package de.unidue.continuityguide;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.opencsv.CSVReader;


public class Maker {

	static List<Category> prelimCategories = new ArrayList<Category>();
	static List<Category> categories;
	
	static List<Variable> variables = new ArrayList<Variable>();
	
	static List<String> resultCats = new ArrayList<String>();
	
	static String varDirectory = "/Users/Martina/Desktop/continuityguide/vardocs_all";
	static String megaDirectory = "/Users/Martina/Desktop/continuityguide/mega";
	
	public static final String[] LEVEL2 = {
			"203 Political interest",
			"222 Party membership and party activities",
			"227 Electoral campaign",
			"323 Discussion of politics",
			"720 Forming of government, government, opposition, states",
			"788 Politicians",
			"1001 Media use for political information",
			"2847 Other sources for political information",
			"2 Election participation",
			"7194 Internet use",
			"26 Voting decision",
			"130 Party preference and identification",
			"1067 Level of political information",
			"1109 Important political tasks",
			"1116 Domestic policy",
			"1152 Moral and religious issues",
			"1169 Education policy",
			"1175 Economic issues",
			"360 General partisan attitudes",
			"390 Perceived party images and differences",
			"1277 Social welfare",
			"1307 Law and order",
			"1337 Defense and disarmament",
			"1368 Environmental protection, energy policy and technical development",
			"1391 Foreign policy",
			"3011 Political participation (no parties)",
			"1498 Other specific issues",
			"1520 Questions about the reunification in 1990",
			"1649 Political systems",
			"3056 Federal state",
			"1695 Attitude towards governmental and non-governmental institutions",
			"1718 General attitudes towards parties, political leaders and the party system",
			"1742 Socio-psychological measures",
			"1814 Regional identification",
			"1818 Identification with religious community",
			"1839 Identification with a social stratum",
			"1845 Identification with other movements",
			"1862 Personal and family characteristics",
			"1896 Geographical location and neighborhood characteristics",
			"1918 Education",
			"1933 Occupation and job related variables",
			"1987 Financial situation, possessions and social security",
			"2033 Socio-economic status",
			"2037 Military experiences/roles",
			"2040 Non-political organizational membership",
			"2064 Leisure time activities",
			"2103 Demographic characteristics of Germany",
			"2117 R's consumption of daily newspapers, weekly magazines, magazines",
			"2128 Interest in and consumption of radio broadcasts",
			"2134 Interest in and consumption of television broadcasts",
			"2144 Who was present during the interview?",
			"2145 Day the interview was obtained",
			"2147 Date of interview",
			"2148 Day of interview",
			"2149 Daytime of interview",
			"2150 Duration of interview",
			"2151 R's co-operation during the interview",
			"2152 Did R follow the interview with interest?",
			"2153 R's recall of last interview (interviewer rating)",
			"2154 Month of survey",
			"2155 Interview method",
			"2157 Number of waves R has been interviewed in",
			"2161 Days R was home before the interview",
			"2169 Where has R previously been interviewed?",
			"2174 Year",
			"2176 Weight",
			"2177 Study number",
			"2178 Respondent number",
			"2179 Administrative District Identification Code, Community number, Community identification, district, zones", 
			"2180 Filter",
			"2188 Interviewer identification",
			"2192 Person-to-person communication",
			"2209 Family and kin",
			"2220 Neighbors",
			"2223 Miscellaneous",
			"4822 Questions about the Nazi era, first and second World War, division of Germany",
			"4851 Telephone number in Federal state",
			"4853 Enquiry period",
			"4854 Postcode",
			"4945 Most important current topics in the media",
			"5024 Occupation zone",
			"5031 Other",
			"5153 Trust in the media",
			"5154 Opinion on Bild-Zeitung",
			"5171 Satisfaction with the radio programme",
			"9266 Interference by another present person",
			"9267 Version number"
	};
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		parseCategories();
		fillChildCats();
		parseVariables();
		
		//makeVariableDocs();
		//makeMegaDocs();
		
	}
	
	private static void makeMegaDocs() {

		File sourceDirectory = new File("/Users/Martina/Desktop/continuityguide/train");
		File[] xmldocs = sourceDirectory.listFiles();
		ArrayList<File> fileList = new ArrayList<File>(Arrays.asList(xmldocs));
		
		for (String category: LEVEL2) {           
			String ids = "";
			String studynos = "";
			String varinos = "";
			String labels = "";
			String qtexts = "";

			for(int i=0; i < fileList.size(); i++) {
				String filepath = fileList.get(i).getPath();
				Variable v = parse(filepath);
				//System.out.println(v.toString());
				//System.out.println(getCategoryName(v.finalcat));
				//System.out.println(category);
				
				if (v.finalcat.equals(category)) {
					ids = ids + " " + v.id;
					studynos = studynos + " " + v.studyno;
					varinos = varinos + " " + v.varino;
					labels = labels + " " + v.label;
					qtexts = qtexts + " " + v.qtext;
					System.out.println("asdf");
				};

			}

			Element megadoc = new Element("megadoc");
			Document document = new Document(megadoc); 

			megadoc.addContent(new Element("ids").setText(ids));
			megadoc.addContent(new Element("studynos").setText(studynos));
			megadoc.addContent(new Element("varinos").setText(varinos));
			megadoc.addContent(new Element("labels").setText(labels));
			megadoc.addContent(new Element("qtexts").setText(qtexts));

			XMLOutputter xmlOutput = new XMLOutputter();  
			try {
				String cleancategory0 = category.replace("/ ", "");
				String cleancategory = cleancategory0.replace("/", " ");
				//System.out.println(cleancategory);
				xmlOutput.setFormat(Format.getPrettyFormat());
				xmlOutput.output(document, new FileWriter(  
						megaDirectory + "/" + cleancategory + ".xml"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
		}
	}
	
	private static void makeVariableDocs() {
		for (int i=0; i<2000; i++) {
			Variable v = variables.get(i);
			
			resultCats.clear();
			resultCats.add(v.leafcategory);
			getCategoriesForDoc(v.leafcategory, resultCats);
			Collections.reverse(resultCats);
			v.categories = resultCats;
			if (resultCats.size() > 2) { 
				v.finalcat = resultCats.get(2);
			} else if (resultCats.size() > 1) {
				v.finalcat = resultCats.get(1);
			} else {
				v.finalcat = resultCats.get(0);
			}
			System.out.println(v.toString());
				
			Element vardoc = new Element("vardoc");
			Document document = new Document(vardoc); 

			vardoc.addContent(new Element("id").setText(v.id));
			vardoc.addContent(new Element("studyno").setText(v.studyno));
			vardoc.addContent(new Element("varino").setText(v.varino));
			vardoc.addContent(new Element("label").setText(v.label));
			vardoc.addContent(new Element("qtext").setText(v.qtext));
			vardoc.addContent(new Element("category").setText(getCategoryName(v.finalcat)));

			XMLOutputter xmlOutput = new XMLOutputter();  
			try {
				xmlOutput.setFormat(Format.getPrettyFormat());
				xmlOutput.output(document, new FileWriter(  
						varDirectory + "/" + v.id + ".xml"));
				System.out.println(v.id);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			
		}
	}
	
	
	private static String getCategoryName(String number) {
		
		for (String s : LEVEL2) {
			if (s.startsWith(number + " ")) {
				return s;
			}
		}
		
		return "no";
	}
	
	private static void getCategoriesForDoc(String inCat, List<String> resultCats) {
		
		for (Category c : categories) {
			if (c.childCats.contains(inCat)) {
				resultCats.add(c.id);
				getCategoriesForDoc(c.id, resultCats);
			}
		}
		
	}
	
	private static void parseCategories() {
		String csvFile = "/Users/Martina/Desktop/continuityguide/tkey_Category.csv";

        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader(csvFile), ';');
            String[] line;
            while ((line = reader.readNext()) != null) {
            	Category category = new Category(line[1], line[4], line[2]);
            	prelimCategories.add(category);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	
	private static void fillChildCats() {
		

		List<Category> newList = new ArrayList<Category>();
		List<String> level1Cats = new ArrayList<String>();
		level1Cats.add("1000");
		level1Cats.add("1");
		level1Cats.add("1108");
		level1Cats.add("358");
		level1Cats.add("1648");
		level1Cats.add("1813");
		level1Cats.add("1861");
		level1Cats.add("2116");
		level1Cats.add("2143");
		level1Cats.add("2191");
		Category newCat1 = new Category("0", "root", "0", level1Cats);
		newList.add(newCat1);
		
		
		
		for (Category c : prelimCategories) {
			
			List<String> childCats = new ArrayList<String>();
			
			for (Category cd : prelimCategories) {
				if (cd.parentCat.equals(c.id)) {
					childCats.add(cd.id);
				}
			}
			
			Category newCat = new Category(c.id, c.name, c.parentCat, childCats);
			newList.add(newCat);
		}
		
		categories = newList;
		
	}
	
	
	private static void parseVariables() {
		String csvFile = "/Users/Martina/Desktop/continuityguide/trel_VariableCategory.csv";

        CSVReader reader = null;
        try {
            reader = new CSVReader(new FileReader(csvFile), ';');
            String[] line;
            while ((line = reader.readNext()) != null) {
            	Variable variable = new Variable(line[0], line[1], line[4], null, 
            			null, line[5], null, null);
            	variables.add(variable);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        String csvFile2 = "/Users/Martina/Desktop/continuityguide/tkey_Variable.csv";
        
        CSVReader reader2 = null;
        try {
            reader2 = new CSVReader(new FileReader(csvFile2), ';');
            String[] line;
            while ((line = reader2.readNext()) != null) {
            	
            	for (Variable v : variables) {
            		if (line[0].equals(v.studyno) && line[1].equals(v.varino)) {
            			v.label = line[9];
            			v.qtext = line[10];
            		}
            	}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public static Variable parse(String inFile) {
		org.jdom2.Document doc = new org.jdom2.Document();
		try {
			doc = new SAXBuilder().build(inFile);
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
			return null;
		}
		Element root = doc.getRootElement();
		String id = root.getChildText("id");
		String studyno = root.getChildText("studyno");
		String varino = root.getChildText("varino");
		String label = root.getChildText("label");
		String qtext = root.getChildText("qtext");
		String category = root.getChildText("category");

		
		return new Variable(studyno, varino, id, label, qtext, null, null, category);
	}
	
	public static class Category {
		String id;
		String name;
		String parentCat;
		List<String> childCats = new ArrayList<String>();
		
		public Category(String id, String name, String parentCat) {
			this.id = id;
			this.name = name;
			this.parentCat = parentCat;
		}
		
		public Category(String id, String name, String parentCat, List<String> childCats) {
			this.id = id;
			this.name = name;
			this.parentCat = parentCat;
			this.childCats = childCats;
		}
		
		public String toString() {
			String string = id + " " + name + " " + parentCat + " " + childCats;
			return string;
		}
		
		public String toString2() {
			String string = id + " " + name;
			return string;
		}
	}
	
	
	public static class Variable {
		String studyno;
		String varino;
		String id;
		String label;
		String qtext;
		String leafcategory;
		List<String> categories;
		String finalcat;
		
		public Variable(String studyno, String varino, String id, String label, String qtext, String leafcategory, List<String> categories, String finalcat) {
			this.studyno = studyno;
			this.varino = varino;
			this.id = id;
			this.label = label;
			this.qtext = qtext;
			this.leafcategory = leafcategory;
			this.categories = categories;
			this.finalcat = finalcat;
		}
		
		public String toString() {
			return new String(studyno + " " + varino + " " + id + " " + label + " " + "qtext here" + " " + leafcategory + " " + categories + " " + finalcat);
		}
		
	}

}
