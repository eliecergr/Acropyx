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

import org.codehaus.groovy.grails.commons.ConfigurationHolder

@MultiTenant
class Competition {

    static enum Type {
        Solo,Synchro
    }

    String code
    String name
    Type type
    Date startTime
    Date endTime
    Boolean isAPWC

    SortedSet markCoefficients
    SortedSet judges
    SortedSet runs



    static hasMany = [
        markCoefficients: MarkCoefficient,
        judges: Judge,
        runs: Run ]

    static constraints = {
        code(blank:false, size: 3..15, unique: 'tenantId')
        name()
        type()
        markCoefficients()
        judges()
        startTime(nullable: true, format: ConfigurationHolder.config.ch.acropyx.dateFormat)
        endTime(nullable: true, format: ConfigurationHolder.config.ch.acropyx.dateFormat)
        runs()
        isAPWC(nullable: true)

        markCoefficients(validator: { coefficients ->
            if (coefficients) {
                def sum = 0.0
                coefficients.each { coefficient ->
                    sum += coefficient.coefficient
                }
                if (sum != 1.0) {
                    return "sum"
                }
            }
            return true
        }
        )
    }

    static mapping = {
        isAPWC(defaultValue: true)
    }

    static transients = ["active", "ended"]

    def boolean isActive() {
        return (startTime != null) && (endTime == null)
    }

    def boolean isEnded() {
        return (startTime != null) && (endTime != null)
    }

    def start() {
        if (isActive()) {
            throw new RuntimeException("Competition '${this}' is already started");
        }
        if (Competition.countByStartTimeIsNotNullAndEndTimeIsNull() >= 2) {
            throw new RuntimeException("Maximum 2 active competitions");
        }

        if (!startTime) {
            startTime = new Date()
        }
        endTime = null
    }

    def end() {
        if (startTime == null) {
            throw new RuntimeException("Competition '${this}' is not yet started");
        }
        if (endTime != null) {
            throw new RuntimeException("Competition '${this}' is already terminated");
        }
        endTime = new Date();
    }

    def findActiveRuns() {
        return runs.findAll { it.isActive() };
    }

    def findStartedRuns() {
        return runs.findAll { it.isStarted() };
    }

    def findEndedRuns() {
        return runs.findAll { it.isEnded() }.sort(it.endTime);
    }

   // def findFirstRun() {
   //     return runs.sortBystartTime()[0]
   // }

    def addDefaultCoefficients() {
        def markDefinition = MarkDefinition.findByName("Technical expression")
        if (markDefinition) {
            if (type == Type.Solo) {
                new MarkCoefficient(competition:this, markDefinition:markDefinition, coefficient:0.4).save()
            } else {
                new MarkCoefficient(competition:this, markDefinition:markDefinition, coefficient:0.3).save()
            }
        }
        markDefinition = MarkDefinition.findByName("Choreography")
        if (markDefinition) {
            if (type == Type.Solo) {
                new MarkCoefficient(competition:this, markDefinition:markDefinition, coefficient:0.4).save()
            } else {
                new MarkCoefficient(competition:this, markDefinition:markDefinition, coefficient:0.3).save()
            }
        }
        markDefinition = MarkDefinition.findByName("Landing")
        if (markDefinition) {
            if (type == Type.Solo) {
                new MarkCoefficient(competition:this, markDefinition:markDefinition, coefficient:0.2).save()
            } else {
                new MarkCoefficient(competition:this, markDefinition:markDefinition, coefficient:0.2).save()
            }
        }
        markDefinition = MarkDefinition.findByName("Synchronization")
        if (markDefinition) {
            if (type == Type.Synchro) {
                new MarkCoefficient(competition:this, markDefinition:markDefinition, coefficient:0.2).save()
            }
        }
    }

    def addAllJudges() {
        Judge.list().each() { judge -> addToJudges(judge) }
        save()
    }

    def String toString() {
        return code;
    }

    def computeResults() {
        def competitorResults = []
        findStartedRuns().each { endedRun ->
            endedRun.findEndedFlights().each { flight ->
                // If exists, return the competitor who has already a run in 'competitorResults'
                def competitorResult = competitorResults.find { it.competitor == flight.competitor }
                if (!competitorResult) {
                    competitorResult = [:]
                    competitorResult.flights = [:]
                    competitorResults.add(competitorResult)
                }

                competitorResult.competitor = flight.competitor
                def flightResult = [:]
                flightResult.id = flight.id
                //flightResult.result = flight.computeResult(flight.computeDetailedResults())
                flightResult.result = flight.computeFlightResult(false)

                competitorResult.flights.put(endedRun.id, flightResult)

                if (!competitorResult.overall) {
                    competitorResult.overall = 0d
                }
                competitorResult.overall = competitorResult.overall + flightResult.result

                if (!competitorResult.warnings) {
                    competitorResult.warnings = 0d
                }
                competitorResult.warnings = competitorResult.warnings + flight.warnings

                if (competitorResult.warnings >= 3) {
                    competitorResult.overall =   0d
                }
            }
        }

        // Sort by rank (overall mark)
        return competitorResults.sort { it.overall }.reverse()
    }


    def double computeCompetitorResult(Competitor competitor){
        def points = 0
        Run.findAllByCompetitionAndEndTimeIsNotNull(this).each { run ->
            def flight = Flight.findByRunAndCompetitor(run, competitor)
            if (flight != null){
                points += flight.computeFlightResult(false)
            }
        }
        return points
    }


    def int countFlights(Competitor competitor){
        int count = 0
        runs.each {run ->
            if (Flight.findByCompetitorAndRun(competitor, run) != null){
                count++;
            }
        }

        return count;
    }

    def double computeCompetitorAverageResult(Competitor competitor){
        def count = this.countFlights(competitor);
        if (count > 0 ){
            return   computeCompetitorResult(competitor)/ count  //Run.countByCompetitionAndEndTimeIsNotNull(this)
        }

        return 0;
    }

    def double computeCompetitorCompensation(Competitor competitor){
        def points = 0
        Run.findAllByCompetitionAndEndTimeIsNotNull(this).each { run ->
            points += (run as Run).calculateCompetitorCompensation(competitor)
        }
        return points
    }

    def double computeCompetitorResultWithCompensation(Competitor competitor){

        def compensation =   computeCompetitorCompensation(competitor)
        def competitorResult = computeCompetitorResult(competitor)

        return competitorResult +  compensation
    }





}
