import java.util.HashMap;
import java.util.Map;

public class DocumentInfo {
	HashMap<String,Integer> wordCount = new HashMap<String, Integer>();
	HashMap<String, Double> tfidf = new HashMap<String, Double>();
	int maxFreq = 0;
	int totalWordCount = 0;
	
	public int getTotalWordCount() {
		return totalWordCount;
	}

	public void setTotalWordCount(int totalWordCount) {
		this.totalWordCount = totalWordCount;
	}

	void addWord(String word)
	{
		if(!wordCount.containsKey(word))
			wordCount.put(word,1);
		else
			wordCount.put(word, wordCount.get(word)+1);
		
		if(wordCount.get(word)>maxFreq)
			maxFreq = wordCount.get(word);
		
	}
	
	void computeTFIDF(int numberOfDocs, Map<String, Integer> IDF)
	{
		for(String word: wordCount.keySet())
		{
			double tf = 0.5 + ((0.5*wordCount.get(word))/maxFreq);
			
			double idf= (double)numberOfDocs/(1+IDF.get(word));
			
			this.tfidf.put(word, tf*idf);
		}
	}

}
