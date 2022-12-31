
package com.beyondrelations.db.rfd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.stream.Stream;
import java.util.List;
import java.util.Map;

//import com.alibaba.fastjson.JSON;

/**
Runs sql scripts in the working directory using the supplied workflo json run config with database information.
*/
public class RfdInit
{

	private Path workfloRoot;


	public static void main(
			String[] args
	) {
		new RfdInit().runScripts( "F:\\shared\\autoplan\\backups\\12-31-2022_mor9" );
	}


	public RfdInit(
	) {
		;
	}


	private void runScripts(
			String directoryPathStringWithSqlScripts
	) {
		try {
			Path directoryPathWithSqlScripts = Paths.get( directoryPathStringWithSqlScripts );

			DirectoryStream<Path> allFilesOfCurrentDirectory = Files.newDirectoryStream( directoryPathWithSqlScripts );
			for ( Path someFilePath : allFilesOfCurrentDirectory ) {
				File someFile = someFilePath.toFile();
				if ( ! someFile.isFile()
							|| ! someFile.getName().endsWith( "sql" ) )
						continue;
				runSqlScript( someFilePath );
			}
		}
		catch ( IOException | InvalidPathException ipe ) {
			System.err.println( ipe );
		}
	}


	private void runSqlScript(
			Path sqlFile
	) {
		String
			pathToMysqlExecutable = "",
			hostIpv4Address = "",
			hostPort = "",
			serverUser = "",
			serverPassword = "",
			datbaseName = "";
		List<String> commandComponents = new LinkedList<>();
		commandComponents.add( pathToMysqlExecutable );
		commandComponents.add( "--protocol=tcp" );
		commandComponents.add( "--default-character-set=utf8" );
		commandComponents.add( "--host="+ hostIpv4Address );
		commandComponents.add( "--port="+ hostPort );
		commandComponents.add( "--user="+ serverUser );
		commandComponents.add( "--password="+ password );
		//commandComponents.add( "-p" );
		//commandComponents.add( "--execute=\"source "+ sqlFile.toAbsolutePath().toString() +"\"" ); // not clear why this didn't work instead
		commandComponents.add( "-e" );
		commandComponents.add( "\"source "+ sqlFile.toAbsolutePath().toString() +"\"" );
		commandComponents.add( datbaseName );

		ProcessBuilder yourJar = new ProcessBuilder( commandComponents );
		yourJar.directory( sqlFile.getParent().toFile() );
		yourJar.inheritIO();
		try {
			yourJar.start().waitFor();
		}
		catch ( IOException ie ) {
			System.err.println( "Couldn't run sql because "+ ie );
		}
		catch ( InterruptedException ie ) {
			System.err.println( "Programmatically told to quit via "+ ie );
			return;
		}
	}
}














