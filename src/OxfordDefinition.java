
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import javax.net.ssl.HttpsURLConnection;

/**
 * This class is for educational purposes only.<br>
 * This class pulls oxford definitions into local files, so don't use it.<br>
 * Oxford says you cant save its definitions.
 * @author Travis Pavelka
 */
public class OxfordDefinition {
	// the way dates are parsed and written in the file
	public static final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("d MMMM yyyy");

	public String lang;
	private int reqsTotal = 0;
	private int reqsMonth = 0;
	private int reqsSession = 0;
	private int reqsMin = 0;
	
	public String getOxfordRequest(String language, String word) {
		// make sure the formatting is correct for the word input
		word.toLowerCase();
		word.replaceAll(" ", "_");
		word.trim();
		
		// the file location for this language and word
		File file_request = new File(language, word+".txt");
		
		// the definition data to return
		String str_request = "";
		
		if(file_request.exists()) {
			// the request has been made
			
			// get the request from hdd
			try {
				// open up the file
				BufferedReader filereader = new BufferedReader(new FileReader(file_request));
				
				// the current line of text
				String line;
				
				// get the data line by line
				while((line = filereader.readLine()) != null) {
					str_request += line + System.lineSeparator();
				}
				
				// close IO
				filereader.close();
				
				// the previously saved definition data
				return str_request;
				
			} catch(IOException e) {
				e.printStackTrace();
				return null;
			}
			
		} else {
			// the request has not been made
			
			// create the request directory from the parent directory
			new File(file_request.getParent()).mkdirs();
			
			try {
				// create the file
				file_request.createNewFile();
			} catch(IOException e) {
				e.printStackTrace();
			}
			
			// test legality
			if(this.reqsMin < 60 && this.reqsTotal < 1000) {
				// init vars for credentials
				String app_id;
				String app_key;
				
				// get the authentication required by the Oxford API
				File creds = new File("creds.txt");
				try {
					BufferedReader credread = new BufferedReader(new FileReader(creds));
					app_id = credread.readLine().split(":")[1];
					app_key = credread.readLine().split(":")[1];
					credread.close();
				} catch(IOException e) {
					e.printStackTrace();
					return null;
				}
				
				// the definition URL
				String str_url = "https://od-api.oxforddictionaries.com/api/v2/entries/" + language + "/" + word;
				
				// the oxford url connection object
				HttpsURLConnection urlConnection;
				
				try {
					// set up the connection
					URL url = new URL(str_url);
					urlConnection = (HttpsURLConnection) url.openConnection();
					urlConnection.setRequestProperty("Accept", "application/json");
					urlConnection.setRequestProperty("app_id", app_id);
					urlConnection.setRequestProperty("app_key", app_key);
				} catch(IOException e) {
					System.out.println("Failed to connect to the oxford servers.");
					return null;
				}
				
				// the connection succeeded
				
				try {
					// read the output from the server
					BufferedReader oxreader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
					
					// a line of data from the server
					String line;
					while ((line = oxreader.readLine()) != null) {
						
						// create the request line by line from the server stream
						str_request += line + System.lineSeparator();
					}
					
					// close the connection to the server
					urlConnection.disconnect();
					oxreader.close();
					
					// save the definition
					// increment the static request counters
					this.reqsTotal++;
					this.reqsSession++;
					this.reqsMin++;
					
					// save the request to a new file
					BufferedWriter oxwriter = new BufferedWriter(new FileWriter(file_request));
					oxwriter.write(str_request.toString());
					oxwriter.close();
					
					// get the reqs file string
					BufferedReader limreader = new BufferedReader(new FileReader(FILE_MONTHLY_OXFORD_REQUESTS));
					
					// the old oxford request counter string
					String str_ox_req = "";
					
					// read the whole reqs file
					String chr = null;
					int read = 0;
					while((read = limreader.read()) != -1) {
						chr = String.valueOf((char) read);
						str_ox_req += chr;
					}
					
					// close IO
					limreader.close();
					
					// get today's date
					Calendar today = new GregorianCalendar();
					Date date = today.getTime();
					String str_todays_date = DATE_PARSER.format(date);
					
					// the entries in the reqs file
					String[] entries = str_ox_req.split("[:\\n]");
					
					// the new reqs file string
					String str_write = "";
					
					// write to the reqs file
					BufferedWriter limwriter;
					
					// test if the old file contained today's date
					if(str_ox_req.contains(str_todays_date)) {
						// there was an entry for today
						
						// open the file to write
						limwriter = new BufferedWriter(new FileWriter(FILE_MONTHLY_OXFORD_REQUESTS));
						
						// search for today's entry and prepare for overwrite
						for (int i = 0; i < entries.length; i++) {
							
							// for even index values
							if(i%2 == 0) {
								
								// where the date = today
								if(entries[i].equals(str_todays_date)) {
									
									// overwrite today's entry
									entries[i+1] = String.valueOf(this.reqsSession);
								}
							}
							
							// prepare for overwrite
							str_write += entries[i]+":";
						}
						
						// write back to the file
						limwriter.write(str_write);
						
					} else {
						// there was no entry for today
						
						// open the file for appending
						limwriter = new BufferedWriter(new FileWriter(FILE_MONTHLY_OXFORD_REQUESTS, true));
						
						// create a new entry for today's date
						str_write += str_todays_date+":"+this.reqsSession + System.lineSeparator();
						
						// write to the file
						limwriter.write(str_write);
					}
					
					// close the monthly oxford requests file
					limwriter.close();
					
					// return the definiton
					return str_request;
					
				} catch(IOException e) {
					e.printStackTrace();
					return null;
				}
				
			} else {
				// dont do the request if its not legal
				return null;
			}
		}
	}
	
	/**
	 * Uses the IANA language code for languages.<br>
	 * Use "en-us" for american english.
	 * Use "en-gb" for british english.
	 */
	public OxfordDefinition(String lang) {
		// set the language
		this.lang = lang;
		
		// create a file to overwrite with
		File temp_file = new File("temp.txt");
		
		try {
			// delete old requests
			BufferedReader limreader = new BufferedReader(new FileReader(FILE_MONTHLY_OXFORD_REQUESTS));
			BufferedWriter limwriter = new BufferedWriter(new FileWriter(temp_file));
			
			// get today's date
			Calendar a_month_ago = new GregorianCalendar();
			a_month_ago.setLenient(true);
			
			// 30 days in the past
			a_month_ago.add(Calendar.DAY_OF_MONTH, a_month_ago.get(Calendar.DAY_OF_MONTH) - 30);
			
			// the format of the dates in the file
			String line = limreader.readLine();
			
			while(line != null) {
				// get the date from the line
				Date date_line = DATE_PARSER.parse(line.split("[:]")[0]);
				
			    // delete old dates
			    if(a_month_ago.before(date_line)) {
					line = limreader.readLine();
			    	continue;
			    } else {
			    	limwriter.write(line + System.getProperty("line.separator"));
					line = limreader.readLine();
			    }
			}
			
			// close IO
			limwriter.close(); 
			limreader.close(); 
			
			// delete the old reqs file
			FILE_MONTHLY_OXFORD_REQUESTS.delete();
			
			// overwrite the old file
			temp_file.renameTo(FILE_MONTHLY_OXFORD_REQUESTS);
			
			// count the number of valid requests
			String str_requests = "";
			limreader = new BufferedReader(new FileReader(FILE_MONTHLY_OXFORD_REQUESTS));
			Boolean had_content = false;
			while ((line = limreader.readLine()) != null) {
				str_requests += line+":";
				if(had_content == false) {
					had_content = true;
				}
			}
			
			// close IO
			limreader.close();
			
			if(had_content) {
				// split the file string
				str_requests = str_requests.substring(0, str_requests.lastIndexOf(":"));
				
				// count the number of requests
				String[] array_file = str_requests.split(":");
				
				// iterate through the requests of the file
				for (int i = 1; i < array_file.length; i+=2) {
					this.reqsMonth += (int) Integer.parseInt(array_file[i]);
				}
			} else {
				this.reqsMonth = 0;
			}
		} catch(IOException e) {
			e.printStackTrace();
		} catch(ParseException e) {
			e.printStackTrace();
		}
		
		// a timer for resetting on the minute
		Timer requests_per_minute = new Timer();
		requests_per_minute.schedule(new MinuteRefreshTask(), 60000);
	}
	
	public static void main(String args[]) {
		// get the definition and output it
		if(args[0] != null) {
			String lang = "en-us";
			OxfordDefinition pull = new OxfordDefinition(lang);
			System.out.println("Requesting "+args[0]+":"+System.lineSeparator());
			String out = pull.getOxfordRequest(lang, args[0]);
			System.out.println(out);
			System.exit(0);
		} else {
			System.out.println("Please specify a definition to search for.");
			System.exit(1);
		}
	}
	
	/**
	 * Max reqs per month is 6000
	 */
	private static File FILE_MONTHLY_OXFORD_REQUESTS = new File("monthly_oxford_reqs.txt");
	static {
		// create file if it doesnt exist
		if(!FILE_MONTHLY_OXFORD_REQUESTS.exists()) {
			try {
				FILE_MONTHLY_OXFORD_REQUESTS.createNewFile();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class MinuteRefreshTask extends TimerTask {
		@Override
		public void run() {
			reqsMin = 0;
		}
	}
}
