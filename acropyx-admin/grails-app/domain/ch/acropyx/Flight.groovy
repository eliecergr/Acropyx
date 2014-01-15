/*******************************************************************************
 * Copyright (c) 2011 epyx SA.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package ch.acropyx

import grails.plugin.multitenant.core.groovy.compiler.MultiTenant

import java.util.Date

import org.codehaus.groovy.grails.commons.ConfigurationHolder

@MultiTenant
class Flight {

    Date startTime
    Date endTime

    Competitor competitor
    Run run

    List manoeuvres
    int warnings

    double computeResult

    static hasMany = [manoeuvres: Manoeuvre, marks: Mark]

    static constraints = {
        competitor(blank: false)
        run(blank: false, unique: 'competitor')
        startTime(nullable: true, format: ConfigurationHolder.config.ch.acropyx.dateFormat)
        endTime(nullable: true, format: ConfigurationHolder.config.ch.acropyx.dateFormat)
        warnings(range:0..3)
        computeResult(nullable: true)
    }

    static transients = ["active", "ended"]

    def boolean isActive() {
        return (startTime != null) && (endTime == null)
    }

    def boolean isEnded() {
        return (startTime != null) && (endTime != null)
    }

    def start() {
        if (endTime != null) {
            throw new RuntimeException("Flight '${this}' is terminated");
        }
        if (startTime != null) {
            throw new RuntimeException("Flight '${this}' is already started");
        }
        if ((run == null) || (run.isActive() == false)) {
            throw new RuntimeException("There is no active run");
        }
        if (Flight.countByStartTimeIsNotNullAndEndTimeIsNull() >= 1) {
            throw new RuntimeException("Maximum 1 active flight");
        }

        startTime = new Date();
    }

    def end() {
        if (startTime == null) {
            throw new RuntimeException("Flight '${this}' is not yet started");
        }
        if (endTime != null) {
            throw new RuntimeException("Flight '${this}' is already terminated");
        }
        endTime = new Date();
        // CHECK!!!!!
        computeFlightResult(true);
    }

    def addWarning() {
        warnings++
        if (warnings > 3) {
            warnings = 3;
        }
	computeFlightResult(isEnded())
    }

    def removeWarning() {
        warnings--
        if (warnings < 0) {
            warnings = 0;
        }
	computeFlightResult(isEnded())
    }

    def Map computeDetailedResults() {
        def competition = run.competition
        def markCoefficients = competition.markCoefficients

        def FAIs = competition.judges.findAll { it.role == Judge.Role.FAI }
        def VIPs = competition.judges.findAll { it.role == Judge.Role.VIP }

        def manoeuvresMean = 0.0d
        manoeuvresMean = calculateManoeuvresMean()

        def detailedResults = [:]
        markCoefficients.each {markCoefficient ->
            def markDefinition = markCoefficient.markDefinition

            // FAI judges
            def FAISum = 0d
            FAIs.each { judge ->
                def mark = Mark.searchMark(this, judge, markDefinition)
                if (mark) {
                    if (markDefinition.name == "Technical expression") {
                        FAISum += mark.mark * manoeuvresMean
                    }
                    else {
                        FAISum += mark.mark
                    }
                }
            }
            def FAIMark = FAISum / FAIs.size()

            // VIP judges
            def VIPSum = 0d
            VIPs.each { judge ->
                def mark = Mark.searchMark(this, judge, markDefinition)
                if (mark) {
                    if (markDefinition.name == "Technical expression") {
                        VIPSum += mark.mark * manoeuvresMean
                    }
                    else {
                        VIPSum += mark.mark
                    }
                }
            }
            def VIPMark = VIPSum / VIPs.size()

            def mark;
            if ((FAIs.size() > 0) && (VIPs.size() > 0)) {
                mark = FAIMark * 0.8d + VIPMark * 0.2d
            } else if (FAIs.size() > 0) {
                mark = FAIMark
            } else if (VIPs.size() > 0) {
                mark = VIPMark
            } else {
                mark = 0d
            }

            //discount 10% peer duplicate manoeuvers


            if ((markDefinition.name == "Choreography") &&
                    (run.penality != 0)) {
                def duplicateManoeuvres = DuplicateManoeuvreCount()

                mark = mark * (100 - duplicateManoeuvres * run.penality) / 100

                if (mark < 0) {
                    mark = 0
                }

            }

            detailedResults.put(markCoefficient.id, mark)
        }

        return detailedResults
    }

Integer DuplicateManoeuvreCount() {
        int duplicateManoeuvres = 0
        if (this.manoeuvres.size() > 0){
            def competition = run.competition
            def List<Manoeuvre> manoeuvreList = []
            if (competition != null){
                for (currentRun in competition.runs) {
                    if ((currentRun as Run).isEnded() && run.startTime > (currentRun as Run).startTime){
                        for (flight in  (currentRun as Run).flights ){
                            if (flight.competitor.id == this.competitor.id && flight.endTime < this.startTime){
                                manoeuvreList.addAll(flight.manoeuvres)
                            }
                        }
                    }
                }
            }
            def Map  currentFlightManoeuvres = [:]
            for (int i=0 ; i < this.manoeuvres.size(); i++) {
              Manoeuvre m = this.manoeuvres.get(i) as Manoeuvre;
              def Boolean duplicateManoeuvre;
             // if(allowDuplicate(m)){
             //    def count = 0;
             //    manoeuvreList.each {mano->
             //        if(mano.name == m.name){
             //            count++;
             //        }
             //    }
             //     duplicateManoeuvre = (count > 2);
             //  }

             // else{
            //   duplicateManoeuvre = manoeuvreList.contains(m)
             // }
              duplicateManoeuvre = (!allowDuplicate(m) && manoeuvreList.contains(m))
              def Boolean currentDuplicateManoeuvre =  currentFlightManoeuvres.containsKey(m.id)
              if ( (currentDuplicateManoeuvre && !duplicateManoeuvre) ||
                   (!currentDuplicateManoeuvre && duplicateManoeuvre)){
                  duplicateManoeuvres++
              }
                currentFlightManoeuvres.put(m.id, m)
            }
        }
        return duplicateManoeuvres
    }

    def boolean allowDuplicate(Manoeuvre mano){
        if (mano.name == 'DYN. FULL STALL') return true;
        if (mano.name == 'FULL STALL') return true;
        if (mano.name == 'PITCH PENDULUM') return true;
        if (mano.name == 'TAIL SLIDE') return true;

        return false;
    }

/*
    Integer DuplicateManoeuvreCount() {


        int duplicateManoeuvres = 0

        def competition = run.competition

        if (competition != null){
            //Find duplicate manoeuvres in previous runs
            for (currentRun in competition.runs) {
                if ((currentRun as Run).isEnded() && run.startTime > (currentRun as Run).startTime){
                    for (flight in  (currentRun as Run).flights ){
                        if (flight.competitor.id == this.competitor.id && flight.endTime < this.startTime){
                            manoeuvres.each() { manoeuvre ->
                                if (flight.manoeuvres.contains(manoeuvre)) {
                                    duplicateManoeuvres++
                                }
                            }
                        }
                    }
                }
            }

            if (this.manoeuvres.size() > 0){
                def Map  map = [:]
                //Find duplicate manoeuvres in the current run
                for (int i=0 ; i < this.manoeuvres.size(); i++) {
                  Manoeuvre m = this.manoeuvres.get(i) as Manoeuvre;
                  map.put(m.id, m.id)
                }

                duplicateManoeuvres += this.manoeuvres.size() - map.size()
            }




        }

        return duplicateManoeuvres

    }

*/
    def double computeFlightResult(boolean recalculate) {
       if (recalculate || computeResult == null || computeResult == 0){
         computeResult =  calculateComputeResult(computeDetailedResults())
       }
       return computeResult
    }

    def double calculateComputeResult(Map detailedResults){
         def sum = 0d
        detailedResults.each { key, value ->
            def markCoefficient = MarkCoefficient.get(key)
            sum += value * markCoefficient.coefficient
        }

        if (warnings == 1) {
            sum = sum - 1
        }
        else if (warnings == 2) {
            sum = sum - 3
        }
        else if (warnings == 3) {
            sum = 0;
        }

        computeResult = (sum > 0 ? sum : 0)

        return computeResult
    }

    def String toString() {
        return sprintf('%s - %s', competitor?.name, run.toString());
    }

    def double calculateManoeuvresMean(){
        def firstManoeuvre = 0.0d
        def secondManoeuvre = 0.0d
        def thirdManoeuvre = 0.0d

        manoeuvres.each() { manoeuvre ->
            if (manoeuvre.coefficient > thirdManoeuvre){
                if(manoeuvre.coefficient > secondManoeuvre){
                    if(manoeuvre.coefficient > firstManoeuvre){
                        thirdManoeuvre = secondManoeuvre
                        secondManoeuvre= firstManoeuvre
                        firstManoeuvre = manoeuvre.coefficient
                    }
                    else{
                        thirdManoeuvre = secondManoeuvre
                        secondManoeuvre = manoeuvre.coefficient
                    }
                }
                else{
                    thirdManoeuvre = manoeuvre.coefficient
                }
            }
        }

        return (firstManoeuvre + secondManoeuvre + thirdManoeuvre) / 3

    }


}
