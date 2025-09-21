
package com.beyondrelations.db.rfd;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
		Scanner input = new Scanner( System.in );

		System.out.print( "path to folder that contains sql files -- " );
		String workingDirectory = input.nextLine();

		if ( workingDirectory.trim().isEmpty()
				&& input.hasNext() ) {
			workingDirectory = input.nextLine().trim();
		}

		if ( workingDirectory.trim().isEmpty() ) {
			workingDirectory = ".";
		}

		new RfdInit().runScripts(
				input, workingDirectory );
	}


	public RfdInit(
	) {
		;
	}


	private DatabaseInformation fromCli(
			Scanner input
	) {
		DatabaseInformation dbInfo = new DatabaseInformation();

		System.out.print( "\npath To Mysql Executable -- " );
		dbInfo.pathToMysqlExecutable = input.nextLine();

		if ( dbInfo.pathToMysqlExecutable.trim().isEmpty()
				&& input.hasNext() ) {
			dbInfo.pathToMysqlExecutable = input.nextLine().trim();
		}

		System.out.print( "\nhost Ipv4 Address -- " );
		dbInfo.hostIpv4Address = input.next();

		System.out.print( "\nhost Port -- " );
		dbInfo.hostPort = input.next();

		System.out.print( "\nserver User -- " );
		dbInfo.serverUser = input.next();

		System.out.print( "\nserver Password -- " );
		dbInfo.serverPassword = input.next(); // assuming no space

		System.out.print( "\ndatbase Name -- " );
		dbInfo.datbaseName = input.next();

		return dbInfo;
	}


	private DatabaseInformation fromJson(
			Scanner input,
			String saysPathToJsonCredentials
	) {
		try {
			Path pathToJsonCredentials = Paths.get(
					saysPathToJsonCredentials );
			List<String> allLinesOfFile = Files.readAllLines(
					pathToJsonCredentials );

			if ( allLinesOfFile.isEmpty() ) {
				System.out.println(
						"\nempty file" );
				return fromCli(
						input );
			}

			StringBuilder buffer = new StringBuilder(
					20 * allLinesOfFile.size() );

			for ( String line : allLinesOfFile ) {
				buffer.append( line ).append( "\n" );
			}

			String entireContent = buffer.toString();
			JSONObject entireConfig = new JSONObject(
					entireContent );
			final String keyDatabaseSection = "database",
					keyDatabaseName = "targetName",
					keyDatabaseUser = "user",
					keyDatabasePassword = "pass",
					keyDatabaseIpv4 = "ipv4",
					keyDatabasePort = "port",
					keyDbAddressSection = "ipAddress";

			if ( ! entireConfig.has(
					keyDatabaseSection ) ) { // ¶ assuming the rest is fine
				System.out.println(
						"\nunrecognized json schema" );
				return fromCli(
						input );
			}

			JSONObject entireDatabaseSection = entireConfig.getJSONObject(
					keyDatabaseSection );
			JSONObject entireAddressSection = entireDatabaseSection.getJSONObject(
					keyDbAddressSection );
			DatabaseInformation dbInfo = new DatabaseInformation();

			dbInfo.hostIpv4Address = entireAddressSection.getString(
					keyDatabaseIpv4 );
			dbInfo.hostPort = Integer.toString(
					entireAddressSection.getInt(
								keyDatabasePort ) );

			dbInfo.serverUser = entireDatabaseSection.getString(
					keyDatabaseUser );
			dbInfo.serverPassword = entireDatabaseSection.getString(
					keyDatabasePassword );
			dbInfo.datbaseName = entireDatabaseSection.getString(
					keyDatabaseName );

			System.out.print( "\npath To Mysql Executable -- " );
			dbInfo.pathToMysqlExecutable = input.nextLine();

			return dbInfo;
		}
		catch (
				IOException | InvalidPathException ie
		) {
			System.err.println(
					"\nproblem with reading json file, "+ ie );
			return fromCli(
					input );
		}
		catch (
				JSONException je
		) {
			System.err.println(
					"\nproblem with json content "+ je );
			return fromCli(
					input );
		}
	}


	private DatabaseInformation getDatabaseInfo(
			Scanner input
	) {
		System.out.print( "\npath to json credentials (blank to enter individually) -- " );
		String pathToJsonCredentials = input.nextLine();

		if ( pathToJsonCredentials.trim().isEmpty()
				&& input.hasNext() ) {
			pathToJsonCredentials = input.nextLine().trim();
		}

		if ( pathToJsonCredentials.isEmpty()
				|| ! pathToJsonCredentials.endsWith(
						"json" ) ) {
			return fromCli(
					input );
		}
		else {
			return fromJson(
					input, pathToJsonCredentials );
		}
	}


	private void runScripts(
			Scanner input,
			String directoryPathStringWithSqlScripts
	) {
		DatabaseInformation dbInfo = getDatabaseInfo(
				input );

		try {
			Path directoryPathWithSqlScripts = Paths.get(
					directoryPathStringWithSqlScripts );
			DirectoryStream<Path> allFilesOfCurrentDirectory = Files.newDirectoryStream(
					directoryPathWithSqlScripts );

			for ( Path someFilePath : allFilesOfCurrentDirectory ) {
				File someFile = someFilePath.toFile();

				if ( ! someFile.isFile()
						|| ! someFile.getName().endsWith(
									"sql" ) )
						continue;

				System.out.println(
						"\n\t"+ someFilePath );
				runSqlScript(
						someFilePath, dbInfo );
			}

			System.err.println(
					"\nFinished" );
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














