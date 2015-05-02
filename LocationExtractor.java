import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import com.bericotech.clavin.ClavinException;
import com.bericotech.clavin.GeoParser;
import com.bericotech.clavin.GeoParserFactory;
import com.bericotech.clavin.gazetteer.GeoName;
import com.bericotech.clavin.resolver.ResolvedLocation;


public class LocationExtractor {

	/**
	 * @param args
	 * @throws Exception 
	 */
	GeoParser geoParser;
	
	public LocationExtractor(String indexPath) throws ClavinException
	{
		this.geoParser= GeoParserFactory.getDefault(indexPath);
	}
	
	
	public static void main(String[] args) throws Exception{
		LocationExtractor extractor = new LocationExtractor("lib/IndexDirectory");
		List<GeoData> temp = extractor.getLocationInfoFromFile(new File("PLC-24-proceedings-GD-34.pdf"));
		for(GeoData d: temp)
		{
			System.out.println(d.getGeoName()+":"+d.getLatitude()+","+d.getLongitude());
		}
	}
	
	List<GeoData> getLocationInfoFromFile(File file) throws Exception
	{
		String input = LocationExtractor.tikaReadFileContent(file);
		return parser(input);
		
	}
	
	List<GeoData> getLocationInfoFromStirng(String input) throws Exception
	{
		return parser(input);
	}
	
	/*
	 * Input string data.
	 * Extracts locations from string.
	 * Returns a list of objects with matched location name, latitude and longitude.
	 */
	List<GeoData> parser(String input) throws Exception
	{
		List<ResolvedLocation> locations = this.geoParser.parse(input);
		List<GeoData> data = new ArrayList<GeoData>();
		List<String> dups = new ArrayList<String>();
		for(ResolvedLocation loc: locations)
		{	
			GeoName name = loc.getGeoname();
			String locName = loc.getMatchedName();
			double latitude = name.getLatitude();
			double longitude = name.getLongitude();
			
			if(!dups.contains(locName)){
				GeoData temp = new GeoData(locName, latitude, longitude);
				data.add(temp);
				dups.add(locName);
			}
		}
		
		return data;
	}
	
	
	/*
	 * Input File parameter.
	 * Function reads content from file using tika parser.
	 * returns a string containing data.		
	 */
	static String tikaReadFileContent(File file) throws IOException, SAXException, TikaException
	{
		Parser parser = new AutoDetectParser();
		Metadata metaData = new Metadata();
		ParseContext parseContext = new ParseContext();
		BodyContentHandler handler =  new BodyContentHandler(2147483647);
        FileInputStream is = new FileInputStream(file);
        parser.parse(is, handler, metaData, parseContext);
        //System.out.println("Content: "+ handler.toString());
        return handler.toString();
	}

}
