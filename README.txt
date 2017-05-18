Name: Satish Kumar
UNCC Id: 800966466

Please delete tempOutput and output folders to execute the code again.

Step 1: To do Initial Setup
	$ sudo su hdfs
	$ hadoop fs -mkdir /user/cloudera
	$ hadoop fs -chown cloudera /user/cloudera
	$ exit
	$ sudo su cloudera
	$ hadoop fs -mkdir /user/cloudera/wordcount /user/cloudera/wordcount/input

Step 2. Put all the input files into the new input directory
	
        $ hadoop fs -put canterbury/* /user/cloudera/wordcount/input/

Step 3: To execute DocWordCount.java
	
        $ mkdir -p build
        $ javac -cp /usr/lib/hadoop/*:/usr/lib/hadoop-mapreduce/* DocWordCount.java -d build -Xlint
        $ jar -cvf docwordcount.jar -C build/ .
        $ hadoop jar docwordcount.jar org.myorg.DocWordCount /user/cloudera/wordcount/input /user/cloudera/wordcount/output
        $ hadoop fs -cat /user/cloudera/wordcount/output/*

Step 4: To execute TermFrequency.java
	
        $ hadoop fs -rm /user/cloudera/wordcount/output/*
        $ javac -cp /usr/lib/hadoop/*:/usr/lib/hadoop-mapreduce/* TermFrequency.java -d build -Xlint
        $ jar -cvf termfrequency.jar -C build/ .
        $ hadoop jar termfrequency.jar org.myorg.TermFrequency /user/cloudera/wordcount/input /user/cloudera/wordcount/output
        $ hadoop fs -cat /user/cloudera/wordcount/output/*

Step 5: To execute TFIDF.java
	
	$ hadoop fs -rm /user/cloudera/wordcount/output/*
        
        $ javac -cp /usr/lib/hadoop/*:/usr/lib/hadoop-mapreduce/* TFIDF.java -d build -Xlint
	$ jar -cvf TFIDF.jar -C build/ . 
	$ hadoop jar TFIDF.jar org.myorg.TFIDF /user/cloudera/wordcount/input /user/cloudera/wordcount/tempOutput /user/cloudera/wordcount/output
	$ hadoop fs -cat /user/cloudera/wordcount/output/*

Step 6: To execute Search.java
	//tempOutput stores intermediate output of 1st MapReduce.
	//user is entering the "Search Query" through command line console.
	
	$ hadoop fs -rm /user/cloudera/wordcount/output/*
	$ hadoop fs -rm /user/cloudera/wordcount/tempOutput/*

	$ javac -cp /usr/lib/hadoop/*:/usr/lib/hadoop-mapreduce/* Search.java -d build -Xlint
	$ jar -cvf Search.jar -C build/ . 
	$ hadoop jar Search.jar org.myorg.Search /user/cloudera/wordcount/input /user/cloudera/wordcount/tempOutput1 /user/cloudera/wordcount/tempOutput2 /user/cloudera/wordcount/output
	$ hadoop fs -cat /user/cloudera/wordcount/output/*

Step 6: To execute Rank.java
	//tempOutput1 tempOutput2 stores intermediate output of 1st MapReduce, 2nd MapReduce respectively
        //user is entering the "Search Query" through command line console.
	
	$hadoop fs -rm /user/cloudera/wordcount/output/*
	$hadoop fs -rm /user/cloudera/wordcount/tempOutput1/*
	$hadoop fs -rm /user/cloudera/wordcount/tempOutput2/*

	$ javac -cp /usr/lib/hadoop/*:/usr/lib/hadoop-mapreduce/* TermFrequency.java -d build -Xlint
	$jar -cvf Rank.jar -C build/ . 
	$hadoop jar Rank.jar org.myorg.Rank /user/cloudera/wordcount/input /user/cloudera/wordcount/tempOutput1 /user/cloudera/wordcount/tempOutput2 /user/cloudera/wordcount/tempOutput3 /user/cloudera/wordcount/output
	$hadoop fs -cat /user/cloudera/wordcount/output/*

To execute again:-
       $hadoop fs -rm /user/cloudera/wordcount/output/*
	$hadoop fs -rm /user/cloudera/wordcount/tempOutput1/*
	$hadoop fs -rm /user/cloudera/wordcount/tempOutput2/*
        $hadoop fs -rm /user/cloudera/wordcount/tempOutput3/*

