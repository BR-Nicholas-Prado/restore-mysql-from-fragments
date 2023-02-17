
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
import java.util.Scanner;

//import com.alibaba.fastjson.JSON;

/**
Runs sql scripts in the working directory using the supplied workflo json run config with database information.
*/
public class RfdInit
{

	private class DatabaseInformation {
		String
			pathToMysqlExecutable = "",
			hostIpv4Address = "",
			hostPort = "",
			serverUser = "",
			serverPassword = "",
			datbaseName = "";
	}

	private Path workfloRoot;


	public static void main(
			String[] args
	) {
		if ( args.length < 1 ) {
			System.out.println( "needs start arg for working folder" );
			return;
		}
		String workingDirectory = args[ 0 ];
		if ( workingDirectory.endsWith( ".jar" ) )
			workingDirectory = args[ 1 ];
		new RfdInit().runScripts( workingDirectory );
	}


	public RfdInit(
	) {
		;
	}


	private DatabaseInformation fromCli(
	) {
		DatabaseInformation dbInfo = new DatabaseInformation();
		Scanner input = new Scanner( System.in );
		System.out.print( "pathToMysqlExecutable -- " );
		dbInfo.pathToMysqlExecutable = input.next();
		System.out.print( "hostIpv4Address -- " );
		dbInfo.hostIpv4Address = input.next();
		System.out.print( "hostPort -- " );
		dbInfo.hostPort = input.next();
		System.out.print( "serverUser -- " );
		dbInfo.serverUser = input.next();
		System.out.print( "serverPassword -- " );
		dbInfo.serverPassword = input.next();
		System.out.print( "datbaseName -- " );
		dbInfo.datbaseName = input.next();
		return dbInfo;
	}


	private void runScripts(
			String directoryPathStringWithSqlScripts
	) {
		DatabaseInformation dbInfo = fromCli();
		try {
			Path directoryPathWithSqlScripts = Paths.get( directoryPathStringWithSqlScripts );
			DirectoryStream<Path> allFilesOfCurrentDirectory = Files.newDirectoryStream( directoryPathWithSqlScripts );
			for ( Path someFilePath : allFilesOfCurrentDirectory ) {
				File someFile = someFilePath.toFile();
				if ( ! someFile.isFile()
							|| ! someFile.getName().endsWith( "sql" ) )
						continue;
				System.out.println( someFilePath );
				runSqlScript( someFilePath, dbInfo );
			}
		}
		catch ( IOException | InvalidPathException ipe ) {
			System.err.println( ipe );
		}
	}


	private void runSqlScript(
			Path sqlFile, DatabaseInformation dbInfo
	) {
		List<String> commandComponents = new LinkedList<>();
		commandComponents.add( dbInfo.pathToMysqlExecutable );
		commandComponents.add( "--protocol=tcp" );
		commandComponents.add( "--ssl-mode=DISABLED" );
		commandComponents.add( "--default-character-set=utf8" );
		commandComponents.add( "--host="+ dbInfo.hostIpv4Address );
		commandComponents.add( "--port="+ dbInfo.hostPort );
		commandComponents.add( "--user="+ dbInfo.serverUser );
		commandComponents.add( "--password="+ dbInfo.serverPassword );
		commandComponents.add( "-e" );
		commandComponents.add( "\"source "+ sqlFile.toAbsolutePath().toString() +"\"" );
		commandComponents.add( dbInfo.datbaseName );

		ProcessBuilder process = new ProcessBuilder( commandComponents );
		process.directory( sqlFile.getParent().toFile() );
		process.inheritIO();
		try {
			process.start().waitFor();
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














