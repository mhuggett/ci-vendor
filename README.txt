
* 
Runnable Jar to add current SC assignments based on a TEXT column.
The SCs are added as a new column at the right end of the table.
*


The TEST dir contains the ready-to-deploy runnable Jar, in the required dir structure. XL files to be tested go in the data/test/ folder. The XL files are overwritten, with a SUFFIX added.

Current SC assignments are taken from the HOST set in the data/config.txt.
The config also sets the file SUFFIX, as well as the column names to read from (COL_IN) and write to (COL_OUT).


* BUILD

In the root directory, run --
mvn clean install

Then copy the resulting Jar to TEST with --
cp target/ci-vendor-0.0.1-SNAPSHOT-jar-with-dependencies.jar TEST/ci-vendor-sc-col.jar

And copy the config file with --
cp data/config.txt TEST/data/


* RUN

Double-click the Jar file.
Runtime logging goes to log.txt.

The app can be repeatedly run on the same files. No changes will occur unless the HOST mappings have changed.

# --

