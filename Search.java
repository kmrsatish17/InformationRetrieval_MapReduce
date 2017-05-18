package org.myorg;

/*
 * Name - Satish Kumar
 * 
 * Email ID - skumar34@uncc.edu
 * 
 * 
 * */

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

public class Search extends Configured implements Tool {

	private static final Logger LOG = Logger.getLogger(Search.class);

	public static String query = "";

	public static void main(String[] args) throws Exception {

		// For getting the user query through command line
		Scanner sc = new Scanner(System.in);
		System.out.println("Enter the query : ");
		query = sc.nextLine();

		int res = ToolRunner.run(new Search(), args);

		System.exit(res);
	}

	// Setting job to run the Map and Reduce task
	public int run(String[] args) throws Exception {

		int result = 0;
		
		// setting up job1 to run first Map and Reduce task. It generates the first intermediate result
		Job job1 = Job.getInstance(getConf(), " wordcount ");
		job1.setJarByClass(this.getClass());

		Configuration conf = new Configuration();
		FileInputFormat.addInputPaths(job1, args[0]);
		FileOutputFormat.setOutputPath(job1, new Path(args[1]));
		job1.setMapperClass(Map.class);
		job1.setReducerClass(Reduce.class);
		job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(IntWritable.class);

		// to get the number of files in the input folder and setting into config
		FileSystem numberOfFile = FileSystem.get(job1.getConfiguration());
		FileStatus[] status = numberOfFile.listStatus(new Path(args[0]));
		conf.set("doccount", Integer.toString(status.length));

		// Setting the user query into the config
		conf.set("query", query);

		// setting up job2 to run second Map and Reduce task. Output of first job is given as input to 2nd job. It generates the second intermediate result
		Job job2 = Job.getInstance(conf, " wordcount ");
		job2.setJarByClass(this.getClass());

		FileInputFormat.addInputPaths(job2, args[1]);
		FileOutputFormat.setOutputPath(job2, new Path(args[2]));
		job2.setMapperClass(MapSec.class);
		job2.setReducerClass(ReduceSec.class);
		job2.setOutputKeyClass(Text.class);
		job2.setOutputValueClass(Text.class);

		// setting up job3 for third Map and Reduce task. Output of second job is given as input to 3rd job. It generates the final result
		Job job3 = Job.getInstance(conf, " wordcount ");
		job3.setJarByClass(this.getClass());

		FileInputFormat.addInputPaths(job3, args[2]);
		FileOutputFormat.setOutputPath(job3, new Path(args[3]));
		job3.setMapperClass(MapThird.class);
		job3.setReducerClass(ReduceThird.class);
		job3.setOutputKeyClass(Text.class);
		job3.setOutputValueClass(Text.class);
		
		// Logic to first complete the job1, then job2 and then job3
		if (job1.waitForCompletion(true)) {
			
			if(job2.waitForCompletion(true)){

			result = job3.waitForCompletion(true) ? 0 : 1;
			
			}
		}
		return result;

	}

	// Mapper class for first mapper
	public static class Map extends
			Mapper<LongWritable, Text, Text, IntWritable> {
		private final static IntWritable one = new IntWritable(1);
		private Text word = new Text();

		private static final Pattern WORD_BOUNDARY = Pattern
				.compile("\\s*\\b\\s*");

		public void map(LongWritable offset, Text lineText, Context context)
				throws IOException, InterruptedException {

			// Getting the filename
			String fileName = ((FileSplit) context.getInputSplit()).getPath()
					.getName();

			String line = lineText.toString();
			Text currentWord = new Text();

			for (String word : WORD_BOUNDARY.split(line)) {
				if (word.isEmpty()) {
					continue;
				}
				currentWord = new Text(word + "#####" + fileName);
				context.write(currentWord, one);
			}
		}
	}

	// Reducer class for first reducer
	public static class Reduce extends
			Reducer<Text, IntWritable, Text, DoubleWritable> {
		@Override
		public void reduce(Text word, Iterable<IntWritable> counts,
				Context context) throws IOException, InterruptedException {
			int sum = 0;
			for (IntWritable count : counts) {
				sum += count.get();
			}

			// Calculating the term frequency
			double logTF = 1 + Math.log10(sum);

			context.write(word, new DoubleWritable(logTF));
		}
	}

	// Mapper class for second mapper
	public static class MapSec extends Mapper<LongWritable, Text, Text, Text> {

		private Text word = new Text();

		private static final Pattern WORD_BOUNDARY = Pattern
				.compile("\\s*\\b\\s*");

		public void map(LongWritable offset, Text lineText, Context context)
				throws IOException, InterruptedException {

			String line = lineText.toString();

			// Get the key, value for the mapper required output
			line = line.replace("\t", "=");
			String key = line.substring(0, line.indexOf("#"));
			String value = line.substring(line.lastIndexOf("#") + 1);

			// Write the required output
			context.write(new Text(key), new Text(value));
		}
	}

	// Reducer class for second reducer
	public static class ReduceSec extends
			Reducer<Text, Text, Text, DoubleWritable> {
		@Override
		public void reduce(Text word, Iterable<Text> counts, Context context)
				throws IOException, InterruptedException {

			// getting the number of files in the input folder
			Configuration conf = context.getConfiguration();
			Double size = Double.parseDouble(conf.get("doccount"));

			double count = 0.0;
			ArrayList<String> value = new ArrayList<String>();

			for (Text t : counts) {
				String val = t.toString();
				count++;
				value.add(val);
			}

			// logic for TFIDF value
			for (int i = 0; i < value.size(); i++) {
				String fname = value.get(i);
				String file = fname.substring(0, fname.lastIndexOf("="));
				Double val = Double.parseDouble(fname.substring(fname
						.indexOf("=") + 1));

				// Calculatinf the TFIDF value
				double tfidf = (Math.log10(1 + (size / count))) * val;

				// Writing the second reducer output
				context.write(new Text(word + "#####" + file),
						new DoubleWritable(tfidf));
			}
		}

	}

	
	// Mapper class for third mapper
	public static class MapThird extends Mapper<LongWritable, Text, Text, Text> {

		private Text word = new Text();

		private static final Pattern WORD_BOUNDARY = Pattern
				.compile("\\s*\\b\\s*");

		public void map(LongWritable offset, Text lineText, Context context)
				throws IOException, InterruptedException {

			String line = lineText.toString();

			// Logic to get the filename and value
			String wordInFile = line.substring(0, line.indexOf("#"));
			String fileName = line.substring(line.lastIndexOf("#") + 1,
					line.lastIndexOf("\t"));
			String value = line.substring(line.lastIndexOf("\t") + 1);

			// getting the user query from config
			Configuration config = context.getConfiguration();
			String queryText = (config.get("query"));

			StringTokenizer st = new StringTokenizer(queryText, " ");

			// Comapring the words in query wuth the words in document. If match found then we write the output of third mapper with key as fileName and value as TFIDF score 
			while (st.hasMoreTokens()) {
				if (st.nextToken().equals(wordInFile)) {

					context.write(new Text(fileName), new Text(value));

				}

			}

		}
	}

	// Reducer class for third reducer
	public static class ReduceThird extends
			Reducer<Text, Text, Text, DoubleWritable> {
		@Override
		public void reduce(Text word, Iterable<Text> counts, Context context)
				throws IOException, InterruptedException {

			double total = 0.0;

			// summing the TFIDF values
			for (Text t : counts) {
				
			Double value =  Double.parseDouble(t.toString());
				total = total + value;
			}

			// writing the output of third reducer with key as fileName and value as total TFIDF score
			context.write(new Text(word), new DoubleWritable(total));
			
		}

	}
}
