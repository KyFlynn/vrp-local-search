package solver.cp;

import ilog.cp.*;

import ilog.concert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.lang.StringBuilder;

public class CPInstance {
  // Business parameters
  int numWeeks;
  int numDays;  
  int numEmployees;
  int numShifts;
  int numIntervalsInDay;
  int[][] minDemandDayShift;
  int minDailyOperation;
  
  // Employee parameters
  int minConsecutiveWork;
  int maxDailyWork;
  int minWeeklyWork;
  int maxWeeklyWork;
  int maxConsecutiveNightShift;
  int maxTotalNightShift;

  // ILOG CP Solver
  IloCP cp;
  
  /**
   * Constructor + parser
   */
  public CPInstance(String fileName) {
    try {
      Scanner read = new Scanner(new File(fileName));
      
      while (read.hasNextLine()) {
        String line = read.nextLine();
        String[] values = line.split(" ");
        if (values[0].equals("Business_numWeeks:")) {
          numWeeks = Integer.parseInt(values[1]);
        }
        else if (values[0].equals("Business_numDays:")) {
          numDays = Integer.parseInt(values[1]);
        }
        else if (values[0].equals("Business_numEmployees:")) {
          numEmployees = Integer.parseInt(values[1]);
        }
        else if (values[0].equals("Business_numShifts:")) {
          numShifts = Integer.parseInt(values[1]);
        }
        else if (values[0].equals("Business_numIntervalsInDay:")) {
          numIntervalsInDay = Integer.parseInt(values[1]);
        }
        else if (values[0].equals("Business_minDemandDayShift:")) {
          int index = 1;
          minDemandDayShift = new int[numDays][numShifts];
          for(int d=0; d<numDays; d++)
            for(int s=0; s<numShifts; s++)
              minDemandDayShift[d][s] = Integer.parseInt(values[index++]);
        }
        else if (values[0].equals("Business_minDailyOperation:")) {
          minDailyOperation = Integer.parseInt(values[1]);
        }
        else if (values[0].equals("Employee_minConsecutiveWork:")) {
          minConsecutiveWork = Integer.parseInt(values[1]);
        }
        else if (values[0].equals("Employee_maxDailyWork:")) {
          maxDailyWork = Integer.parseInt(values[1]);
        }
        else if (values[0].equals("Employee_minWeeklyWork:")) {
          minWeeklyWork = Integer.parseInt(values[1]);
        }
        else if (values[0].equals("Employee_maxWeeklyWork:")) {
          maxWeeklyWork = Integer.parseInt(values[1]);
        }
        else if (values[0].equals("Employee_maxConsecutiveNigthShift:")) {
          maxConsecutiveNightShift = Integer.parseInt(values[1]);
        }
        else if (values[0].equals("Employee_maxTotalNigthShift:")) {
          maxTotalNightShift = Integer.parseInt(values[1]);
        }
      }
    }

    catch (FileNotFoundException e) {
      System.out.println("Error: file not found " + fileName);
    }
  }

  /**
   * Solve!
   */
  public String solve() {
    try {
      cp = new IloCP();
      cp.setParameter(IloCP.IntParam.Workers, 1);
      cp.setParameter(IloCP.DoubleParam.TimeLimit, 30);
      // cp.setParameter(IloCP.DoubleParam.TimeLimit, 300);
      cp.setParameter(IloCP.IntParam.SearchType, IloCP.ParameterValues.DepthFirst);   
      cp.setParameter(IloCP.IntParam.LogVerbosity, IloCP.ParameterValues.Terse);   

      // ================================================================================================
      // MODEL
      // ================================================================================================

      int NIGHT_START = 0;
      int NIGHT_END = 8;
      int DAY_START = 8;
      int DAY_END = 16;
      int EVENING_START = 16;
      int EVENING_END = 24;
      
      assert (numWeeks * 7 == numDays);
      assert (numIntervalsInDay == 24);

      // Construct data array
      IloIntVar[][][] data = new IloIntVar[numEmployees][numDays][3];
      for (int emp = 0; emp < numEmployees; emp++) {
        for (int day = 0; day < numDays; day++) {
          data[emp][day][0] = cp.intVar(0, numIntervalsInDay);
          data[emp][day][1] = cp.intVar(0, maxDailyWork);
          data[emp][day][2] = cp.intVar(0, 3);
        }
      }
      
      // Start times imply shifts
      for (int emp = 0; emp < numEmployees; emp++) {
        for (int day = 0; day < numDays; day++) {
          // Night shift == 1
          cp.add(cp.imply(
            cp.and(
              cp.le(0, data[emp][day][0]), 
              cp.ge(7, data[emp][day][0])),
            cp.eq(1, data[emp][day][2])
          ));
          // Day shift == 2
          cp.add(cp.imply(
            cp.and(
              cp.le(8, data[emp][day][0]), 
              cp.ge(15, data[emp][day][0])),
            cp.eq(2, data[emp][day][2])
          ));
          // Evening shift == 3
          cp.add(cp.imply(
            cp.and(
              cp.le(16, data[emp][day][0]), 
              cp.ge(23, data[emp][day][0])),
            cp.eq(3, data[emp][day][2])
          ));
          // Off shift == 0
          cp.add(cp.equiv(
            cp.eq(24, data[emp][day][0]),
            cp.eq(0, data[emp][day][2])
          ));
          // Off shift works no hours
          cp.add(cp.equiv(
            cp.eq(24, data[emp][day][0]),
            cp.eq(0, data[emp][day][1])
          ));
        }
      }

      // After starting a [night/day/evening] shift, can't work past [8/16/24]
      for (int emp = 0; emp < numEmployees; emp++) {
        for (int day = 0; day < numDays; day++) {
          cp.add(cp.le(
            data[emp][day][1], 
            cp.diff(8, cp.modulo(data[emp][day][0], 8))
          ));
        }
      }

      // After starting, must work more than `minConsecutiveWork` consecutive hours or 0 hours
      for (int emp = 0; emp < numEmployees; emp++) {
        for (int day = 0; day < numDays; day++) {
          cp.add(cp.or(
            cp.ge(data[emp][day][1], minConsecutiveWork),
            cp.eq(data[emp][day][1], 0)
          ));
        }
      }

      // Must work less than `maxDailyWork` hours in a day
      for (int emp = 0; emp < numEmployees; emp++) {
        for (int day = 0; day < numDays; day++) {
          cp.add(cp.le(data[emp][day][1], maxDailyWork));
        }
      }

      // Must work more than `minWeeklyWork` and less than `maxWeeklyWork` hours in a week
      for (int emp = 0; emp < numEmployees; emp++) {
        for (int week = 0; week < numWeeks; week++) {
          IloIntExpr[] cum_hours = new IloIntExpr[7];
          for (int day = week*7; day < (week+1)*7; day++) {
            cum_hours[day%7] = data[emp][day][1];
          }
          cp.add(cp.ge(cp.sum(cum_hours), minWeeklyWork));
          cp.add(cp.le(cp.sum(cum_hours), maxWeeklyWork));
        }
      }

      // Must work at most `maxConsecutiveNightShift` consecutive night shifts
      for (int emp = 0; emp < numEmployees; emp++) {
        for (int day = 0; day < numDays - maxConsecutiveNightShift; day++) {
          IloIntExpr[] consecutiveDays = new IloIntExpr[maxConsecutiveNightShift];
          for (int consec_day = 0; consec_day < maxConsecutiveNightShift; consec_day++) {
            consecutiveDays[consec_day] = data[emp][day+consec_day][2];
          }
          cp.add(cp.le(cp.count(consecutiveDays, 1), maxConsecutiveNightShift));
        }
      }

      // Must work at most `maxTotalNightShift` night shifts in a week
      for (int emp = 0; emp < numEmployees; emp++) {
        for (int week = 0; week < numWeeks; week++) {
          IloIntExpr[] shifts_in_week = new IloIntExpr[7];
          for (int day = week*7; day < (week+1)*7; day++) {
            shifts_in_week[day%7] = data[emp][day][2];
          }
          cp.add(cp.le(cp.count(shifts_in_week, 1), maxTotalNightShift));
        }
      }

      // Total employee hours must be more than `minDailyOperation` per day
      for (int day = 0; day < numDays; day++) {
        IloIntExpr[] total_hours_per_day = new IloIntExpr[numEmployees];
        for (int emp = 0; emp < numEmployees; emp++) {
          total_hours_per_day[emp] = data[emp][day][1];
        }
        cp.add(cp.ge(cp.sum(total_hours_per_day), minDailyOperation));
      }

      // Total employees in a shift must be more than `minDemandDayShift` per day per shift
      for (int day = 0; day < numDays; day++) {
        IloIntExpr[] shifts_per_employee = new IloIntExpr[numEmployees];
        for (int emp = 0; emp < numEmployees; emp++) {
          shifts_per_employee[emp] = data[emp][day][2];
        }
        for (int shift = 0; shift < numShifts; shift++) {
          cp.add(cp.ge(cp.count(shifts_per_employee, shift), minDemandDayShift[day][shift]));
        }
      }

      // In the first 4 days, employees must not repeat shifts
      for (int emp = 0; emp < numEmployees; emp++) {
        IloIntExpr[] first_four_days = new IloIntExpr[4];
        for (int day = 0; day < 4; day++) {
          first_four_days[day] = data[emp][day][2];
        }
        cp.add(cp.allDiff(first_four_days));
      }

      // SYMMETRY BREAKING: give an ordering of start times
      for (int emp = 0; emp < numEmployees-1; emp++) {
        cp.add(cp.le(data[emp][0][1], data[emp+1][0][1]));
      }

      // ================================================================================================
      // SEARCH
      // ================================================================================================

      // Flatten variables
      IloIntVar[] startTimes = new IloIntVar[numEmployees * numDays];
      IloIntVar[] shiftDurations = new IloIntVar[numEmployees * numDays];
      IloIntVar[] shiftTypes = new IloIntVar[numEmployees * numDays];
      for (int emp = 0; emp < numEmployees; emp++) {
        for (int day = 0; day < numDays; day++) {
          startTimes[emp*numDays+day] = data[emp][day][0];
          shiftDurations[emp*numDays+day] = data[emp][day][1];
          shiftTypes[emp*numDays+day] = data[emp][day][2];
        }
      }

      // SHIFT SELECTORS
      IloVarSelector[] varSelShift = new IloVarSelector[2];
      varSelShift[0] = cp.selectSmallest(cp.domainSize());
      varSelShift[1] = cp.selectRandomVar();
      IloValueSelector[] valSelShift = new IloValueSelector[2];
      valSelShift[0] = cp.selectLargest(cp.value());
      valSelShift[1] = cp.selectRandomValue();

      // DURATION SELECTORS
      IloVarSelector[] varSelDuration = new IloVarSelector[2];
      varSelDuration[0] = cp.selectSmallest(cp.domainSize());
      varSelDuration[1] = cp.selectRandomVar();
      IloValueSelector[] valSelDuration = new IloValueSelector[2];
      valSelDuration[0] = cp.selectLargest(cp.value());
      valSelDuration[1] = cp.selectRandomValue();

      // START SELECTORS
      IloVarSelector[] varSelStart = new IloVarSelector[2];
      varSelStart[0] = cp.selectSmallest(cp.domainSize());
      varSelStart[1] = cp.selectRandomVar();
      IloValueSelector[] valSelStart = new IloValueSelector[2];
      valSelStart[0] = cp.selectSmallest(cp.value());
      valSelStart[1] = cp.selectRandomValue();

      // Phases: Shifts => Start times => Durations
      IloSearchPhase[] phases = new IloSearchPhase[3];
      phases[0] = cp.searchPhase(shiftTypes, cp.intVarChooser(varSelShift), cp.intValueChooser(valSelShift));
      phases[1] = cp.searchPhase(shiftDurations, cp.intVarChooser(varSelDuration), cp.intValueChooser(valSelDuration));
      phases[2] = cp.searchPhase(startTimes, cp.intVarChooser(varSelStart), cp.intValueChooser(valSelStart));
      cp.setSearchPhases(phases);

      // Randomized restarts
      double failLimit= 100;
      double growth = 1.1;
      cp.setParameter(IloCP.IntParam.FailLimit, (int) failLimit);
      while (!cp.solve()) {
        cp.setParameter(IloCP.IntParam.RandomSeed, ((int) failLimit) * 69);
        failLimit = failLimit * growth;
        // System.out.println("Restarting: " + failLimit);
        cp.setParameter(IloCP.IntParam.FailLimit, (int) failLimit);
      }
      cp.printInformation();

      // Generate output arrays
      int[][] beginED = new int[numEmployees][numDays];
      int[][] endED = new int[numEmployees][numDays];

      for (int emp = 0; emp < numEmployees; emp++) {
        for (int day = 0; day < numDays; day++) {
          if ((int)cp.getValue(data[emp][day][2]) == 0) {
            beginED[emp][day] = -1;
            endED[emp][day] = -1;
          } else {
            beginED[emp][day] = (int)cp.getValue(data[emp][day][0]);
            endED[emp][day] = (int)cp.getValue(data[emp][day][0]) + (int)cp.getValue(data[emp][day][1]);
          }
        }
      }

      // Output
      StringBuilder sb = new StringBuilder();
      for (int emp  = 0; emp < numEmployees; emp++) {
        for (int day = 0; day < numDays; day++) {
          sb.append(beginED[emp][day] + " " + endED[emp][day]);
          sb.append(" ");
        }
      }
      if (sb.length() > 0)
        sb.deleteCharAt(sb.length() - 1);
      return sb.toString();
    }
    catch (IloException e) {
      System.out.println("Error: " + e);
      return "";
    }
  }
  
 /**
   * Poor man's Gantt chart.
   * author: skadiogl
   *
   * Displays the employee schedules on the command line. 
   * Each row corresponds to a single employee. 
   * A "+" refers to a working hour and "." means no work
   * The shifts are separated with a "|"
   * The days are separated with "||"
   * 
   * This might help you analyze your solutions. 
   * 
   * @param numEmployees the number of employees
   * @param numDays the number of days
   * @param beginED int[e][d] the hour employee e begins work on day d, -1 if not working
   * @param endED   int[e][d] the hour employee e ends work on day d, -1 if not working
   */
  void prettyPrint(int numEmployees, int numDays, int[][] beginED, int[][] endED) {
    for (int e = 0; e < numEmployees; e++) {
      System.out.print("E"+(e+1)+": ");
      if (e < 9) System.out.print(" ");
      for (int d = 0; d < numDays; d++) {
        for(int i=0; i < numIntervalsInDay; i++) {
          if (i%8==0) System.out.print("|");
          if (beginED[e][d] != endED[e][d] && i >= beginED[e][d] && i < endED[e][d]) System.out.print("+");
          else System.out.print(".");
        }
        System.out.print("|");
      }
      System.out.println(" ");
    }
  }

  /**
   * Generate Visualizer Input
   * author: lmayo1
   *
   * Generates an input solution file for the visualizer. 
   * The file name is numDays_numEmployees_sol.txt
   * The file will be overwritten if it already exists.
   * 
   * @param numEmployees the number of employees
   * @param numDays the number of days
   * @param beginED int[e][d] the hour employee e begins work on day d, -1 if not working
   * @param endED   int[e][d] the hour employee e ends work on day d, -1 if not working
   */
   void generateVisualizerInput(int numEmployees, int numDays, int[][] beginED, int[][] endED) {
    String solString = String.format("%d %d %n", numEmployees, numDays);

    for (int d = 0; d < numDays; d++) {
      for (int e = 0; e < numEmployees; e++) {
        solString += String.format("%d %d %n", (int)beginED[e][d], (int)endED[e][d]);
      }
    }

    String fileName = Integer.toString(numDays) + "_" + Integer.toString(numEmployees) + "_sol.txt";

    try {
      File resultsFile = new File(fileName);
      if (resultsFile.createNewFile()) {
        System.out.println("File created: " + fileName);
      } else {
        System.out.println("Overwritting the existing " + fileName);
      }
      FileWriter writer = new FileWriter(resultsFile, false);
      writer.write(solString);
      writer.close();
    } catch (IOException e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }
  }
}
