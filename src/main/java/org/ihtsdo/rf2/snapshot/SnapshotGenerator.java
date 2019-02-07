package org.ihtsdo.rf2.snapshot;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class SnapshotGenerator {

	private File sortedFile;
	private Integer[] columnFilterIxs;
	private String[] columnFilterValues;
	private String date;
	private int componentColumn;
	private int effectiveTimeColumn;
	private File outputFile;

	public SnapshotGenerator(File sortedFile, String date,
			int componentColumn, int effectiveTimeColumn, File outputFile,
			Integer[] columnFilterIxs,String[] columnFilterValues) {
		super();
		this.sortedFile = sortedFile;
		this.date = date;
		this.componentColumn = componentColumn;
		this.effectiveTimeColumn = effectiveTimeColumn;
		this.outputFile = outputFile;
		this.columnFilterIxs=columnFilterIxs;
		this.columnFilterValues=columnFilterValues;
	}

	public void execute(){
		
		try {
			long start1 = System.currentTimeMillis();

			FileInputStream fis = new FileInputStream(sortedFile);
			InputStreamReader isr = new InputStreamReader(fis,"UTF-8");
			BufferedReader br = new BufferedReader(isr);

			double lines = 0;
			String nextLine;
			String header = br.readLine();

			if (outputFile.exists()){
				outputFile.delete();
			}
			FileOutputStream fos = new FileOutputStream( outputFile);
			OutputStreamWriter osw = new OutputStreamWriter(fos,"UTF-8");
			BufferedWriter bw = new BufferedWriter(osw);
			
			bw.append(header);
			bw.append("\r\n");
			
			String prevCompo="";
			String prevLine="";
			String[] splittedLine;
			String[] prevSplittedLine;
			boolean bContinue=true;

			while ((prevLine= br.readLine()) != null) {
				prevSplittedLine =prevLine.split("\t",-1);
				
				if (columnFilterIxs!=null){
					bContinue = true;
					for (int i=0;i<columnFilterIxs.length;i++){
						if (prevSplittedLine[columnFilterIxs[i]].compareTo(columnFilterValues[i])!=0){
							bContinue=false;
							break;
						}
					}
				}
				if (bContinue){
					if (prevSplittedLine[effectiveTimeColumn].compareTo(date)<=0){
						prevCompo=prevSplittedLine[componentColumn];
						break;
					}
				}
			}
			if ( !prevCompo.equals("") ){
				while ((nextLine= br.readLine()) != null) {
					splittedLine = nextLine.split("\t",-1);
					
					if (columnFilterIxs!=null){
						bContinue = true;
						for (int i=0;i<columnFilterIxs.length;i++){
							if (splittedLine[columnFilterIxs[i]].compareTo(columnFilterValues[i])!=0){
								bContinue=false;
								break;
							}
						}
					}
					if (bContinue){
						if(splittedLine[componentColumn].equals(prevCompo)){
							if (splittedLine[effectiveTimeColumn].compareTo(date)<=0){
								prevLine=nextLine;
								
							}
						}else{
							if (splittedLine[effectiveTimeColumn].compareTo(date)<=0){
								bw.append(prevLine);
								bw.append("\r\n");
								prevLine=nextLine;
								prevCompo=splittedLine[componentColumn];
								lines++;
							}
						}
					}
				}
				bw.append(prevLine);
				bw.append("\r\n");
				lines++;
				
			}
			
			bw.close();
			br.close();
			long end1 = System.currentTimeMillis();
			long elapsed1 = (end1 - start1);
			System.out.println("Lines in output file  : " + lines);
			System.out.println("Completed in " + elapsed1 + " ms");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
