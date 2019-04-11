/* BaseballElimination.java
   CSC 226 - Spring 2019
   Assignment 4 - Baseball Elimination Program
   
   This template includes some testing code to help verify the implementation.
   To interactively provide test inputs, run the program with
	java BaseballElimination

	Compile with:
	javac -cp .;algs4.jar BaseballElimination.java
	
   To conveniently test the algorithm with a large input, create a text file
   containing one or more test divisions (in the format described below) and run
   the program with
	java -cp .;algs4.jar BaseballElimination file.txt (Windows)
   or
    java -cp .:algs4.jar BaseballElimination file.txt (Linux or Mac)
   where file.txt is replaced by the name of the text file.
   
   The input consists of an integer representing the number of teams in the division and then
   for each team, the team name (no whitespace), number of wins, number of losses, and a list
   of integers represnting the number of games remaining against each team (in order from the first
   team to the last). That is, the text file looks like:
   
	<number of teams in division>
	<team1_name wins losses games_vs_team1 games_vs_team2 ... games_vs_teamn>
	...
	<teamn_name wins losses games_vs_team1 games_vs_team2 ... games_vs_teamn>

	
   An input file can contain an unlimited number of divisions but all team names are unique, i.e.
   no team can be in more than one division.


   R. Little - 03/22/2019
*/

import edu.princeton.cs.algs4.*;
import java.util.*;
import java.io.File;

//Do not change the name of the BaseballElimination class
public class BaseballElimination {
	
	// We use an ArrayList to keep track of the eliminated teams.
	public ArrayList<String> eliminated = new ArrayList<String>();

	// Declare variables to store the standing data.
	public int numTeams;
	public String[] teamNames;
	public int[] wins;
	public int[] remGames;
	public int[][] gameMatrix;
	public boolean[] eliminatedStatus;
	public int mostWins;
	public int[] numVertices;


	/* BaseballElimination(s)
		Given an input stream connected to a collection of baseball division
		standings we determine for each division which teams have been eliminated 
		from the playoffs. For each team in each division we create a flow network
		and determine the maxflow in that network. If the maxflow exceeds the number
		of inter-divisional games between all other teams in the division, the current
		team is eliminated.
	*/
	public BaseballElimination(Scanner s) {

		// Initialize variables to store the standing data.
		numTeams = s.nextInt();
		teamNames = new String[numTeams]; 
		wins = new int[numTeams];
		remGames = new int[numTeams];
		gameMatrix = new int[numTeams][numTeams];
		eliminatedStatus = new boolean[numTeams];
		numVertices = new int[numTeams];
		mostWins = 0;

		// Fill the numVertices array with enough vertices for the numTeams - 1 plus a source s and sink t.
		Arrays.fill(numVertices, (numTeams + 1));

		// Read in data for the standings from the input file.
		for (int i = 0; i < numTeams; i++) {
			teamNames[i] = s.next();
			wins[i] = s.nextInt();
			remGames[i] = s.nextInt();

			if (wins[i] > mostWins) {
				mostWins = wins[i];
			}

			for (int j = 0; j < numTeams; j++) {
				gameMatrix[i][j] = s.nextInt();
			}
		}

		// Run the network maxFlow based elimination check.
		for (int team = 0; team < numTeams; team++) {
			EliminationCheck(team);
		}
	}

	private void EliminationCheck(int team) {

		// If the team has already been eliminated by the simple elimination check, we can skip the network building step.
		// i.e. if a team is eliminated solely on wins + remaining games.
		if (wins[team] + remGames[team] < mostWins) {
			eliminatedStatus[team] = true;
			eliminated.add(teamNames[team]);
			return;
		}

		int numGames = getNumGames(team);
		numVertices[team] = numVertices[team] + numGames;
		// System.out.println("team " + team + " has " + numVertices[team] + " vertices in its network.");

		int[] teamVertexValues = new int[numTeams];
		boolean passedTeam = false;

		for (int i = 0; i < numTeams; i++) {

			if (team == i) {
				passedTeam = true;
				continue;
			}

			if (passedTeam) {
				teamVertexValues[i] = numGames + i;
			} else {
				teamVertexValues[i] = numGames + i + 1;
			}
		}


		FlowNetwork flowNetwork = new FlowNetwork(numVertices[team]);

		// Initialize the sink t and a counter for the current vertex.
		int t = numVertices[team] - 1;
		int curVertex = 1;
		double capacityFromSource = 0;

		// This nested loop adds the edges between the source s, the game vertices, and the team vertices.
		for (int i = 0; i < numTeams; i++) {
            for (int j = i; j < numTeams; j++) {
                if ((i == j) || (i == team) || (j == team)) {
                    continue;
                }

                // Add an edge connecting the source vertex to the game vertex between team i and team j.
                flowNetwork.addEdge(new FlowEdge(0, curVertex, gameMatrix[i][j]));
                capacityFromSource = capacityFromSource + gameMatrix[i][j];

                // Add two edges connecting the game vertex to the team vertices for team i and team j.
                flowNetwork.addEdge(new FlowEdge(curVertex, teamVertexValues[i], Double.POSITIVE_INFINITY));
                flowNetwork.addEdge(new FlowEdge(curVertex, teamVertexValues[j], Double.POSITIVE_INFINITY));

                curVertex++;
            }
        }

        // This nested loop adds the edges between the team vertices and the sink t.
        for (int teamVertex = 0; teamVertex < numTeams; teamVertex++) {

        	// We don't have a game vertex for the team which this network is for.
        	if (teamVertex == team) {
        		continue;
        	}

        	// Formula for weight of edge between game vertex and sink t.
        	double weight = wins[team] + remGames[team] - wins[teamVertex];

        	// Ensure we have no negative edge weights.
        	if (weight < 0.0) {
        		weight = 0.0;
        	}

        	// Add an edge with the above formula for weight from the team vertex to the sink t.
        	flowNetwork.addEdge(new FlowEdge(teamVertexValues[teamVertex], t, weight));
        }

        // Run the max flow calculation using FordFulkerson on the created network.
        FordFulkerson maxFlowCalc = new FordFulkerson(flowNetwork, 0, t);

        double totalFlow = maxFlowCalc.value();

        if (totalFlow != capacityFromSource) {
        	eliminatedStatus[team] = true;
        	eliminated.add(teamNames[team]);
        }

        //System.out.println(); 
	}

	// Returns the number of game nodes that will be in the network for the selected team.
	private int getNumGames(int team) {
        int numGames = 0;
        for (int i = 0; i < numTeams; i++) {
            for (int j = i; j < numTeams; j++) {
                if ((i != j) && (i != team) && (j != team)) {
                    numGames++;
                }
            }
        }
        return numGames;
    }

	/* main()
	   Contains code to test the BaseballElimination function. You may modify the
	   testing code if needed, but nothing in this function will be considered
	   during marking, and the testing process used for marking will not
	   execute any of the code below.
	*/
	public static void main(String[] args){
		Scanner s;
		if (args.length > 0){
			try{
				s = new Scanner(new File(args[0]));
			} catch(java.io.FileNotFoundException e){
				System.out.printf("Unable to open %s\n",args[0]);
				return;
			}
			System.out.printf("Reading input values from %s.\n",args[0]);
			
			try{
				s = new Scanner(new File(args[0]));
			} catch(java.io.FileNotFoundException e){
				System.out.printf("Unable to open %s\n",args[0]);
				return;
			}
			System.out.printf("Reading input values from %s.\n",args[0]);
			
			long startTime;
			long endTime;
			long duration;

			startTime = System.nanoTime();
			BaseballElimination be = new BaseballElimination(s);		
			endTime = System.nanoTime();
			
			duration = (endTime - startTime)/1000000;
	        System.out.println("Execution time: " + duration + " milliseconds");

			if (be.eliminated.size() == 0)
				System.out.println("No teams have been eliminated.");
			else
				System.out.println("Teams eliminated: " + be.eliminated);
		}else{
			String[] testFiles = {
					"TestFilesA3/teams1.txt",
					"TestFilesA3/teams10.txt",
					"TestFilesA3/teams12-allgames.txt",
					"TestFilesA3/teams12.txt",
					"TestFilesA3/teams24.txt",
					"TestFilesA3/teams29.txt",
					"TestFilesA3/teams30.txt",
					"TestFilesA3/teams32.txt",
					"TestFilesA3/teams36.txt",
					"TestFilesA3/teams4.txt",
					"TestFilesA3/teams42.txt",
					"TestFilesA3/teams48.txt",
					"TestFilesA3/teams4a.txt",
					"TestFilesA3/teams4b.txt",
					"TestFilesA3/teams5.txt",
					"TestFilesA3/teams50.txt",
					"TestFilesA3/teams54.txt",
					"TestFilesA3/teams5a.txt",
					"TestFilesA3/teams5b.txt",
					"TestFilesA3/teams5c.txt",
					"TestFilesA3/teams5d.txt",
					"TestFilesA3/teams60.txt",
					"TestFilesA3/teams7.txt",
					"TestFilesA3/teams8.txt"
			};
			
			for(String testFile : testFiles) {
				try{
					s = new Scanner(new File(testFile));
				} catch(java.io.FileNotFoundException e){
					System.out.printf("Unable to open %s\n",testFile);
					return;
				}
				System.out.printf("Reading input values from %s.\n",testFile);
				
				long startTime;
				long endTime;
				long duration;

				startTime = System.nanoTime();
				BaseballElimination be = new BaseballElimination(s);		
				endTime = System.nanoTime();
				
				duration = (endTime - startTime)/1000;
		        System.out.println("Exectution took: " + duration + " microseconds");

				if (be.eliminated.size() == 0)
					System.out.println("No teams have been eliminated.");
				else
					System.out.println("Teams eliminated: " + be.eliminated);
					System.out.println();
			}
		}
	}
}
