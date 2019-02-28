package zhu.tradecompare;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jxl.Workbook;
import jxl.format.Colour;
import jxl.write.Label;
import jxl.write.WritableCell;
import jxl.write.WritableCellFormat;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class App 
{
	
    public static void main( String[] args )
    {
//    	String csvFilePath = args[0];
//    	String logPath = args[1];
    	String csvFilePath = "C:\\Users\\dzhu\\Desktop\\java\\akoul\\swap.csv";
    	String logPath = "C:\\Users\\dzhu\\Desktop\\java\\akoul\\green rfq logs on qa11 for hour 22.txt";

    	final String [] HEADERS = {
    		"tradeId","timestamp","customerId","defineUserId","firmLei->id","subsidiary","bank",
    		"bankUser","Quote:mtfMakerInfo->firmLei->id","instrument--ccy","instrument--tenor","settlementDate","buySell","Quote:calc(rate)",
    		"Quote:calc(farRate)","amount","farAmount","instrument&isBaseSpecifiedCurrency","estUsdAmount","estUsdFarAmount"
    	};
    	
        ArrayList<String> allCsvString = new ArrayList<String>();

    	try {
	    	
	        File csv = new File(csvFilePath);
	        csv.setReadable(true);
	        csv.setWritable(true);
	        BufferedReader br = null;
	        try {
	            br = new BufferedReader(new FileReader(csv));
	        } catch (FileNotFoundException e) {
	            e.printStackTrace();
	        }
	        String line = "";
	        try {
	            while ((line = br.readLine()) != null)
	            {
	                allCsvString.add( formatNumbersInString(line) );
	            }
	            System.out.println("csv line number=：" + allCsvString.size());
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	
    	List<String> logLines = getDoneTradeLogLines(logPath);
    	printList(logLines);
    	

    	
//        ArrayList<String[]> listParamsInCsv = new ArrayList<String[]>();
        ArrayList<ArrayList<String>> allParamsInLog = new ArrayList<ArrayList<String>>();
    	//for(String csvTrade : allCsvString) {
    	for(int i=2; i<allCsvString.size(); i++) {
    		String csvTrade = allCsvString.get(i);
    		String [] paramsInCsv = csvTrade.split(",");
    		
    		String tradeId = paramsInCsv[1];
    		String logTradeDone = getTradeDoneLog(logLines, tradeId);
    		String logQuoteSubmitted = getQuoteSelectedLog(logLines, tradeId);
    		System.out.println("######################### ");
    		System.out.println("trade id = "+ tradeId);
    		System.out.println("logTradeDone = " + logTradeDone);
    		System.out.println("logQuoteSubmitted = " + logQuoteSubmitted);
    		System.out.println("size of paramsInCsv = " + paramsInCsv.length);
    		printArr(paramsInCsv);

        	//get parameters from text log.
        	ArrayList<String> paramsInLog = getParamsFromLog(HEADERS, logTradeDone, logQuoteSubmitted);
        	allParamsInLog.add(paramsInLog);
        	printList(paramsInLog);
    		System.out.println("size of paramsInLog = " + paramsInLog.size());

    	}
    	
    	String outputFilePath = csvFilePath.replace("csv", "xls");
    	compareAndWriteToExcel(HEADERS, allCsvString, allParamsInLog, outputFilePath);
    	
    }

    public static void compareAndWriteToExcel(String[] header, ArrayList<String> allCsvString, 
    		ArrayList<ArrayList<String>> allParamsInLog, String outputFilePath) {
    	try {
    		//int iRows = allCsvString.size();
	    	File xlsFile = new File(outputFilePath);
	        WritableWorkbook workbook = Workbook.createWorkbook(xlsFile);
	        WritableSheet sheet = workbook.createSheet("sheet1", 0);
	        for (int row = 0; row < 2; row++)
	        {
	        	String [] arr = allCsvString.get(row).split(",");
			    for (int col = 0; col < arr.length; col++)
			    {
			    	sheet.addCell(new Label(col, row, arr[col]));
			    }
	        }
	        int k=0;
	        for (int row = 2; ; row++) {
	        	String [] arr = allCsvString.get(k+2).split(",");
			    for (int col = 0; col < arr.length; col++)
			    {
			    	sheet.addCell(new Label(col, row, arr[col]));
			    }
		    	row ++ ;
			    for (int col = 1; col < arr.length; col++)
			    {
			    	String valFromCsv = arr[col];
			    	String valFromLog = allParamsInLog.get(k).get(col-1);			    	
			    	sheet.addCell(new Label(col, row, valFromLog));
			    	if (compare(header[col-1], valFromCsv, valFromLog) == false) {
			    		WritableCell c = sheet.getWritableCell(col, row);
			    		WritableCellFormat newFormat = new WritableCellFormat();
			    		newFormat.setBackground(Colour.RED);
			    		c.setCellFormat(newFormat);
			    	}
				}
			    k++;
			    if(k==allParamsInLog.size()) {
			    	break;
			    }
	        }
	        workbook.write();
	        workbook.close();
    	}catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    public static boolean compare(String header, String strInCsv, String strInLog) {
    	if("NOT EXISTS".equalsIgnoreCase(strInLog) || "EMPTY".equalsIgnoreCase(strInLog)) {
    		if( "".equals(strInCsv) || "-".equals(strInCsv) ) {
    			return true;
    		}else {
    			return false;
    		}
    	}
    	if("timestamp".equals(header)) {
    		//2019/01/24 22:15:48.609000 UTC
    		//2019-01-24T22:15:48.609641
    		strInCsv = strInCsv.replace("/", "-").substring(0, 23);
    		strInLog = strInLog.replace("T", " ").substring(0, 23);
    		return strInCsv.equals(strInLog);
    	}else if("settlementDate".equals(header)) {
    		try {
	    		Date d1 = new SimpleDateFormat("MM/dd/yyyy").parse(strInCsv);
	    		Date d2 = new SimpleDateFormat("yyyy-MM-dd").parse(strInLog);
	    		return d1.equals(d2);
    		}catch(Exception e) {
    			e.printStackTrace();
    			return false;
    		}
    	}else if(header.contains("mount")) {
    		//amount, farAmount, ...
    		//strInCsv=40000000.00
    		//strInLog=40000000
    		try {
	    		BigDecimal d1 = new BigDecimal(strInCsv);
	    		BigDecimal d2 = new BigDecimal(strInLog);
	    		return d1.compareTo(d2) == 0;
    		}catch(Exception e) {
    			e.printStackTrace();
    			return false;
    		}
    	}
    	return strInCsv.equals(strInLog);
    }
    
    public static String formatNumbersInString(String line) {
    	if(line==null) {
    		return line;
    	}
    	if(!line.contains("\"")) {
    		return line;
    	}
    	String [] arr = line.split("\"");
    	String ret = "";
    	for (int i=0; i<arr.length; i++) {
    		if( i % 2 == 0) {
    			ret = ret + arr[i];
    		}else {
    			ret = ret + arr[i].replace(",", "");
    		}
    	}
    	return ret;
    }
    
    public static ArrayList<String> getParamsFromLog(String[] headers, String logTradeDone, String logQuoteSubmitted){
    	ArrayList<String> ret = new ArrayList<String>();
    	String val;
    	for(String header : headers) {
    		if(header.contains("Quote")) {
    			val = getValue(logQuoteSubmitted, header.replace("Quote:", ""));
    		}else {
    			val = getValue(logTradeDone, header);
    		}
    		ret.add(val);
    	}
    	return ret;
    }
    
    public static String getValue(String logLine, String header) {
    	if(header.contains("->")) {
    		String [] arr = header.split("->");
    		int pos = 0;
    		for(int i = 0; i < arr.length; i++) {
        		pos = logLine.indexOf(wrapQuote(arr[i]), pos) + wrapQuote(arr[i]).length() + 1;//skip ":"
    		}
    		return getValueByPosition(logLine, pos);
    	}else if(header.contains("--")) {
    		//"instrument": "FX-EUR/USD-1W/2W-MTF",
    		String [] arrHeader = header.split("--");
    		int pos = logLine.indexOf(wrapQuote(arrHeader[0]))+wrapQuote(arrHeader[0]).length()+1;
    		String val = getValueByPosition(logLine, pos);
    		String[] arrValues = val.split("-");
    		if("ccy".equals(arrHeader[1])) {
    			return arrValues[1];
    		}else if("tenor".equals(arrHeader[1])) {
    			String ret = arrValues[2];
    			if(arrValues.length==4) {
    				ret = ret +"-"+arrValues[3];
    			}
    			return ret;
    		}
    	}else if(header.contains("&")) {
    		//"instrument&isBaseSpecifiedCurrency"
			//"instrument": "FX-EUR/USD-SP-MTF",
			//"isBaseSpecifiedCurrency": true,
    		String [] arrHeader = header.split("&");
    		int pos = logLine.indexOf(wrapQuote(arrHeader[0]))+wrapQuote(arrHeader[0]).length()+1;
    		String instrument = getValueByPosition(logLine, pos).split("-")[1];//EUR/USD
    		pos = logLine.indexOf(wrapQuote(arrHeader[1]))+wrapQuote(arrHeader[1]).length()+1;
    		String isBase = getValueByPosition(logLine, pos);
    		if ("true".equals(isBase)) {
    			return instrument.split("/")[0];
    		}else {
    			return instrument.split("/")[1];
    		}
    		
    	}else if(header.contains("+")) {
    		String side = getValueBySingleHeader(logLine, "buySell");
    		String [] arrHeader = header.split("\\+");
    		String rate = getValueBySingleHeader(logLine, arrHeader[0]);
    		String points = getValueBySingleHeader(logLine, arrHeader[1]);
    		String retVal = calculateRate(rate, points, side);
    		if(arrHeader.length == 3){
	    		String farPoints = getValueBySingleHeader(logLine, arrHeader[2]);
	    		retVal = calculateRate(retVal, farPoints, side);
	    		if("NOT EXISTS".equalsIgnoreCase(getValueBySingleHeader(logLine, "farAmount"))) {
	    			retVal = "NOT EXISTS";
	    		}
    		}
    		return retVal;
    	}else {
    		return getValueBySingleHeader(logLine, header);
    	}
    	return "EMPTY";
    }
    
    public static String calculateRate(String rate, String points, String side) {
    	try {
    		DecimalFormat df = new DecimalFormat("#.##########");
    		Double d1 = Double.parseDouble(rate);
    		Double d2 = Double.parseDouble(points);
    		if ("BUY".equalsIgnoreCase(side)) {
    			return df.format(d1 + d2);
    		}else {
    			return df.format(d1 - d2);
    		}
    	}catch (Exception e) {
    		e.printStackTrace();
    	}
    	return null;
    }
    
    public static String wrapQuote(String header) {
    	return "\"" + header + "\"";
    }
    
    public static String getValueBySingleHeader(String logLine, String singleHeader) {
    	if( !logLine.contains(singleHeader) ) {
    		return "NOT EXISTS";
    	}
    	int pos = logLine.indexOf(wrapQuote(singleHeader))+wrapQuote(singleHeader).length()+1;
		return getValueByPosition(logLine, pos);
    }
    
    public static String getValueByPosition(String logLine, int startPos) {
    	int endPos = logLine.indexOf("}", startPos);
    	int pos2 = logLine.indexOf(",", startPos);
    	if( pos2>0 && endPos>pos2 ) {
    		endPos = pos2;
    	}
    	String val = logLine.substring(startPos, endPos).trim();
    	val = val.replace("\"", "");
    	if("".equals(val)) {
    		return "EMPTY";
    	}
    	return val;
    }
    

    public static List<String> getDoneTradeLogLines(String logFilePath) {
    	List<String> lines = new ArrayList<String>();
    	try {
    		BufferedReader reader = new BufferedReader (new FileReader(logFilePath));
    		String line;
    		while ((line = reader.readLine())!=null) {
    			if (line.contains("AUTOBANK_ACKED_EXECUTION_TRADE_DONE") || 
    					line.contains("QUOTE_SELECTED_BY_ID")) {
    				lines.add(line);
    			}
    		}
    		reader.close();    		
    	}catch(Exception e) {
    		e.printStackTrace();
    		return null;
    	}
    	return lines;
    }
    
    public static String getTradeDoneLog (List<String> lines, String tradeId) {
    	for(String s : lines) {
    		if(s.contains(tradeId) &&
    				s.contains("AUTOBANK_ACKED_EXECUTION_TRADE_DONE")) {
    			return s;
    		}
    	}
    	return null;
    }
    
    public static String getQuoteSelectedLog (List<String> lines, String tradeId) {
    	for(String s : lines) {
    		if(s.contains(tradeId) &&
    				s.contains("QUOTE_SELECTED_BY_ID")) {
    			return s;
    		}
    	}
    	return null;
    }
    
    public static void printList(List<String> list) {
    	for (String o : list) {
    		System.out.println(o);
    	}
    }
    
    public static void printArr(String [] arr) {
    	System.out.println("Begin printing arr#########");
    	for (String o : arr) {
    		System.out.println(o);
    	}
    	System.out.println("Finish printing arr#########");
    }
}
