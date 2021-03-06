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

import groovyx.net.http.ContentType
import groovyx.net.http.RESTClient

import java.math.RoundingMode
import java.text.DecimalFormat

import javax.annotation.PostConstruct

import org.codehaus.groovy.grails.commons.ConfigurationHolder as CH

class DisplayerService {
    // Base url constant to access the 'Displayer' is 'ch.acropyx.displayerUrl'
    def messageSource

    def RESTClient restClient

    @PostConstruct
    def void init() {
        restClient = new RESTClient( CH.config.ch.acropyx.displayerUrl )
        restClient.handler.success = { "Success!" }
        restClient.handler.failure = { resp -> "Unexpected failure: ${resp.statusLine}" }
    }

    def void competitionHasStarted(String tenant, Competition competition) {
        Object[] args = [competition.name]
        def name = messageSource.getMessage( 'displayer.competition_start.title', args, Locale.default )
        def text = messageSource.getMessage( 'displayer.competition_start.text', args, Locale.default )
        def resp = restClient.post( path : 'waiting',
                body : '{ "name" : "' + name + '", "text" : "' + text + '" }',
                requestContentType : ContentType.JSON,
                headers : ["Tenant": tenant] )
            
      //  sleep(30000)
      //  competitionStartingOrder(tenant, competition)
    }
    
    def void runStartingOrder(String tenant, Run run){
        if (run) {
            def subTitle = messageSource.getMessage( 'displayer.run_start.order', []as Object[], Locale.default )
            Object[] args = [
                run.competition.name,
                run.name,
                subTitle
            ]
            def name = messageSource.getMessage( 'displayer.run_end.title', args, Locale.default )
            //def competitors = generateRunStartingOrder(run, run.competition.id)
            def json = '{ "name" : "' + name + '", "competitors" : ' + generateRunStartingOrder(run, run.competition.id) + '}'
            def resp = restClient.post( path : 'startOrderRun',
                    body : json,
                    requestContentType : ContentType.JSON,
                    headers : ["Tenant": tenant] )
        }
    }

    

    def void competitionHasEnded(String tenant, Competition competition) {
        resultCompetition(tenant, competition)
    }

    def void runHasStarted(String tenant, Run run) {
        Object[] args = [
            run.competition.name,
            run.name
        ]
        def name = messageSource.getMessage( 'displayer.run_start.title', args, Locale.default )
        def text = messageSource.getMessage( 'displayer.run_start.text', args, Locale.default )
        def resp = restClient.post( path : 'waiting',
                body : '{ "name" : "' + name + '", "text" : "' + text + '" }',
                requestContentType : ContentType.JSON,
                headers : ["Tenant": tenant] )
    }

    def void runHasEnded(String tenant, Run run) {
        resultRun(tenant, run)
    }

    def void flightHasStarted(String tenant, Flight flight) {
        Object[] args = [
            flight.run.competition.name,
            flight.run.name
        ]
        def name = messageSource.getMessage( 'displayer.flight_start.title', args, Locale.default )
        def competitor = flight.competitor
        def isPilot = (competitor instanceof Pilot)
        def json = '{ "name" : "' + name + '", "' + (isPilot ? 'competitor' : 'team' ) + '" : ' + competitor.toJSON() + '}'
        def resp = restClient.post( path : (isPilot ? 'currentFlightSolo' : 'currentFlightTeam'),
                body : json,
                requestContentType : ContentType.JSON,
                headers : ["Tenant": tenant] )
    }

    def void flightHasEnded(String tenant, Flight flight, showResult) {
        Object[] args = [
            flight.run.competition.name,
            flight.run.name
        ]
        def name = messageSource.getMessage( 'displayer.flight_end.title', args, Locale.default )
        def competitor = flight.competitor
        def isPilot = (competitor instanceof Pilot)
        def json = '{ "name" : "' + name + '", "' + (isPilot ? 'competitor' : 'team' ) + '" : ' + competitor.toJSON()
        def detailedResults = flight.computeDetailedResults()
        json += ', "marks" : ' + generateDetailedResults(detailedResults)
        json += ', "overall" : { "kind" : "TOTAL POINTS", "value" : "' + roundResult(flight.calculateComputeResult(detailedResults)) + '"}'
        json += ', "warnings" : ' + flight.warnings
        json += ', "rank" : "' + generateRank(flight) + '."}'
        def resp = restClient.post( path : (isPilot ? 'resultFlightSolo' : 'resultFlightTeam'),
                body : json,
                requestContentType : ContentType.JSON,
                headers : ["Tenant": tenant] )


        //if (showResult){
        //    sleep(20000)
         //   resultRun(tenant, flight.run)
       // }


    }

    def void resultRun(String tenant, Run run) {
        if (run) {
            def msgKey = (run.endTime ? 'displayer.result.run.final.text' : 'displayer.result.run.intermediate.text')
            def subTitle = messageSource.getMessage( msgKey, []as Object[], Locale.default )
            Object[] args = [
                run.competition.name,
                run.name,
                subTitle
            ]
            def name = messageSource.getMessage( 'displayer.run_end.title', args, Locale.default )
            def json = '{ "name" : "' + name + '", "competitors" : ' + generateRunResult(run) + '}'
            def resp = restClient.post( path : 'resultRun',
                    body : json,
                    requestContentType : ContentType.JSON,
                    headers : ["Tenant": tenant] )
        }
    }

    def void resultCompetition(String tenant, Competition competition) {
        if (competition) {
            def json = generateAllCompetitionData(competition);
            def resp = restClient.post( path : 'resultRun',
                    body : json,
                    requestContentType : ContentType.JSON,
                    headers : ["Tenant": tenant] )
        }
    }

    def void sendPaf(String tenant, String text, long decay) {
        text = text.replace("\'", "\\\\u0027")
        text = text.replace("\"", "\\\\u0022")
        def resp = restClient.post( path : 'paf',
                body : '{ "text" : "' + text + '", "style" : "decay:' + decay + '" }',
                requestContentType : ContentType.JSON,
                headers : ["Tenant": tenant] )
    }

    def void clearPaf(String tenant) {
        def resp = restClient.post( path : 'pafclean',
                headers : ["Tenant": tenant] )
    }
    
    
    def String generateRunStartingOrder(run, competitionId){
        
        def String json = '['
        def competitors = []


        def competition =  Competition.get(competitionId)
        def results = competition.computeResults()
        if (results.size() > 0){
            results.each { competitorResult ->
                competitors.add(0,competitorResult.competitor)
            }
        }
        else
        {
            competitors = Competitor.competitorsForActiveRun()
        }


        competitors.eachWithIndex() { competitor, i ->
            json += '{ "name" :  "' + competitor.name +'"'
            if ( competitor instanceof Pilot ) {
                json += ', "country" : "' + competitor.toCountryISO3166_1() + '"'
            }
            else{
                def team = competitor as Team
                def pilots  = team.pilots.toList()
                def pilot1 = pilots.get(0)
                def pilot2 = pilots.get(1)

                json += ', "pilot1" : "' + pilot1.name + '"'
                json += ', "pilot2" : "' + pilot2.name + '"'
                json += ', "country1" : "' + pilot1.toCountryISO3166_1() + '"'
                json += ', "country2" : "' + pilot2.toCountryISO3166_1() + '"'
            }

            json += '}'
            if (i < competitors.size() -1) {
                json += ','
            }
        }
        json += ']'          
    }

    def String generateRunResult(Run run) {
        def String json = '['
        def endedFlights = run.findEndedFlights(true)

        endedFlights.eachWithIndex { flight, i ->
            json += '{ "name" : "' + flight.competitor.name + '", "warnings" : '  + flight.warnings + ', "mark" : "' + roundResult(flight.computeFlightResult(false))    + '"'
            if ( flight.competitor instanceof Pilot ) {
                json += ', "country" : "' + flight.competitor.toCountryISO3166_1() + '"'
            }
            else{
                def team = flight.competitor as Team
                def pilots  = team.pilots.toList()
                def pilot1 = pilots.get(0)
                def pilot2 = pilots.get(1)

                json += ', "pilot1" : "' + pilot1.name + '"'
                json += ', "pilot2" : "' + pilot2.name + '"'
                json += ', "country1" : "' + pilot1.toCountryISO3166_1() + '"'
                json += ', "country2" : "' + pilot2.toCountryISO3166_1() + '"'

            }

            json += '}'
            if (i < endedFlights.size() -1) {
                json += ','
            }
        }
        json += ']'
    }

    def String generateCompetitionResult(Competition competition) {
        def String json = '['
        def results = competition.computeResults()

        results.eachWithIndex { result, i ->

            //Check if the competitor is DSQ
            def countWarnings = 0
            if( result.overall == 0 ){
                competition.findEndedRuns().each { endedRun ->
                   countWarnings += Flight.findByCompetitorAndRun(result.competitor, endedRun).warnings
                }
            }
			def isDSQ = 0
			if (countWarnings >= 3){
			   isDSQ = 1
			}

           json += '{ "name" : "' + result.competitor.name + '", "warnings" : '  + result.warnings + ', "isDSQ" : ' + isDSQ + ', "nbRuns" : ' + result.flights.size() + ', "mark" : "' + roundResult(result.overall) + '"'
			 
            if (result.competitor instanceof Pilot) {
                json += ', "country" : "' + result.competitor.toCountryISO3166_1() + '"'
            }
            else{
                def team = result.competitor as Team
                def pilots  = team.pilots.toList()
                def pilot1 = pilots.get(0)
                def pilot2 = pilots.get(1)

                if (pilot1.name.size() > pilot2.name.size())
                {
                    json += ', "pilot1" : "' + pilot1.name + '"'
                    json += ', "pilot2" : "' + pilot2.name + '"'
                    json += ', "country1" : "' + pilot1.toCountryISO3166_1() + '"'
                    json += ', "country2" : "' + pilot2.toCountryISO3166_1() + '"'
                }else{
                    json += ', "pilot1" : "' + pilot2.name + '"'
                    json += ', "pilot2" : "' + pilot1.name + '"'
                    json += ', "country1" : "' + pilot2.toCountryISO3166_1() + '"'
                    json += ', "country2" : "' + pilot1.toCountryISO3166_1() + '"'
                }

            }

            json += '}'
            if (i < results.size() -1) {
                json += ','
            }
        }
        json += ']'
    }

    def String generateDetailedResults(Map detailedResults) {
        def String json = '['
        if (detailedResults) {
            def nbOfResults = detailedResults.size()
            def ii = 0
            detailedResults.each { markCoefficientId, mark ->
                ii++
                def markCoefficient = MarkCoefficient.get(markCoefficientId)
                json += '{ "kind" : "' + markCoefficient.markDefinition.name + '"'
                json += ', "value" : "' + roundResult(mark) + '" }'
                if (ii < nbOfResults) {
                    json += ','
                }
            }
        }
        json += ']'
    }

    def String generateRank(Flight flight) {
        def flights = flight.run.findEndedFlights(true)
        int position = flights.indexOf(flight);
        if (position == -1) {
            return 1
        } else {
            return position + 1
        }
    }

    def String roundResult(result) {
        def decimalFormat = new DecimalFormat("0.000")
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP)
        def markString = decimalFormat.format(result)
        return markString;
    }

    def void showSponsors(String tenant){
        def name =  'Sponsors' //messageSource.getMessage( 'displayer.competition_end.title', args, Locale.default )
        def json = '{ "name" : "' + name + '"}'
        def resp = restClient.post( path : 'showSponsors',
                body : json,
                requestContentType : ContentType.JSON,
                headers : ["Tenant": tenant] )
    }

    def void rotateResults(String tenant, competition1, competition2){

//        def json = '{ "competition1" : ' + generateAllCompetitionData(competition1)  + ','
//                      'competition2": "' + generateAllCompetitionData(competition2)  + '}'

        if (competition1 && competition2) {
            def msgKey1 = (competition1.endTime ? 'displayer.result.competition.final.text' : 'displayer.result.competition.intermediate.text')
            def subTitle1 = messageSource.getMessage( msgKey1, []as Object[], Locale.default )
            Object[] args1 = [competition1.name, subTitle1]
            def name1 = messageSource.getMessage( 'displayer.competition_end.title', args1, Locale.default )

            def msgKey2 = (competition2.endTime ? 'displayer.result.competition.final.text' : 'displayer.result.competition.intermediate.text')
            def subTitle2 = messageSource.getMessage( msgKey2, []as Object[], Locale.default )
            Object[] args2 = [competition2.name, subTitle2]
            def name2 = messageSource.getMessage( 'displayer.competition_end.title', args2, Locale.default )


            def json = '{ "competition1": {"name" : "' + name1 + '", "competitors": ' + generateCompetitionResult(competition1)  + '}, "competition2": {"name" : "' + name2 + '", "competitors" :' + generateCompetitionResult(competition2)  +   '}  }'
            def resp = restClient.post( path : 'rotateResults',
                body : json,
                requestContentType : ContentType.JSON,
                headers : ["Tenant": tenant] )
        }
    }

    //TODO: Add DATE!!!!
    def void overallRanking(String tenant, type){
        def json = ''
        if ( type == Competition.Type.Solo){
            json = generateSoloRankingData()
        }
        else{
            json = generateSyncRankingData()
        }
        def resp = restClient.post( path : 'overallRanking',
                body : json,
                requestContentType : ContentType.JSON,
                headers : ["Tenant": tenant] )
    }

    def void rotateOverallRanking(String tenant){
        def json = '{ "ranking1":' + generateSoloRankingData() + ', "ranking2":' + generateSyncRankingData() + '}'
        def resp = restClient.post( path : 'rotateRanking',
                body : json,
                requestContentType : ContentType.JSON,
                headers : ["Tenant": tenant] )
    }


    def String generateAllCompetitionData(Competition competition)
    {
        def json = '{}';
        if (competition) {
            def msgKey = (competition.endTime ? 'displayer.result.competition.final.text' : 'displayer.result.competition.intermediate.text')
            def subTitle = messageSource.getMessage( msgKey, []as Object[], Locale.default )
            Object[] args = [competition.name, subTitle]
            def name = messageSource.getMessage( 'displayer.competition_end.title', args, Locale.default )
            json = '{ "name" : "' + name + '", "competitors" : ' + generateCompetitionResult(competition) + '}'
        }

        return json;
    }


    def String generateSoloRankingData(){

        def countRun = countRankingRuns(Competition.Type.Solo)
        //TODO: Move to Messages
        def name = 'APWC 2013 Solo <br /> Overall ranking after ' + countRun + ' run'

        if (countRun > 1){
            name += 's'
        }

        Map results = computePilotsRanking(countRun)

        return '{ "name" : "' + name + '", "competitors" : ' + generateRankingData(results) + '}'
    }

    def String generateSyncRankingData()
    {
        def countRun = countRankingRuns(Competition.Type.Synchro)
        //TODO: Move to Messages
        def name = 'APWC Synchro 2013 <br /> Overall ranking after ' + countRun + ' run'
        if (countRun > 1){
            name += 's'
        }

        Map results = computeTeamsRanking(countRun)

        return '{ "name" : "' + name + '", "competitors" : ' + generateRankingData(results) + '}'
    }
    //TODO: Add DATE!!!!
    def String generateRankingData(Map ranking){

        def String json = '['

        ranking.eachWithIndex { result, i ->
            json += '{ "name" : "' + (result.key as Competitor).name + '",  "mark" : "' + roundResult(result.value) + '"'
            if (result.key  instanceof Pilot) {
                json += ', "country" : "' + (result.key as Pilot).toCountryISO3166_1() + '"'
            }
            else{
                def team = result.key as Team
                def pilots  = team.pilots.toList()
                def pilot1 = pilots.get(0)
                def pilot2 = pilots.get(1)

                if (pilot1.name.size() > pilot2.name.size())
                {
                    json += ', "pilot1" : "' + pilot1.name + '"'
                    json += ', "pilot2" : "' + pilot2.name + '"'
                    json += ', "country1" : "' + pilot1.toCountryISO3166_1() + '"'
                    json += ', "country2" : "' + pilot2.toCountryISO3166_1() + '"'
                }else{
                    json += ', "pilot1" : "' + pilot2.name + '"'
                    json += ', "pilot2" : "' + pilot1.name + '"'
                    json += ', "country1" : "' + pilot2.toCountryISO3166_1() + '"'
                    json += ', "country2" : "' + pilot1.toCountryISO3166_1() + '"'
                }

            }

            json += '}'
            if (i < ranking.size() -1) {
                json += ','
            }
        }
        json += ']'
    }

    //TODO: Add DATE!!!!
   def Map computePilotsRanking (countRuns){
       def ranking  = [:];
       Pilot.listOrderById().each { pilot ->
           def points = 0
           def runs = 0
           Flight.findAllByCompetitorAndEndTimeIsNotNull(pilot).each {flight ->
               if (flight.run.isEnded()  && flight.run.competition.isAPWC){
                   points += flight.computeFlightResult(false)
                   runs++
               }
           }

           if (runs < countRuns){
               points += addCompensation(pilot)
           }

           if (points > 0){
            ranking.put(pilot, points)
           }
       }
       return ranking.sort {-it.value}
   }

    //TODO: Add DATE!!!!
    def Map computeTeamsRanking (countRuns){
        def ranking  = [:];
        Team.listOrderById().each { team ->
            def points = 0
            def runs = 0
            Flight.findAllByCompetitorAndEndTimeIsNotNull(team).each {flight ->
                if (flight.run.isEnded() && flight.run.competition.isAPWC){
                    points += flight.computeFlightResult(false)
                    runs++
                }
            }

            if (runs < countRuns){
                points += addCompensation(team)
            }
            if (points > 0){
                ranking.put(team, points)
            }
        }
        return ranking.sort {-it.value}
    }

    def int countRankingRuns(type){
        def runs = 0
        Run.findAllByEndTimeIsNotNull().each{ run ->
            if (run.competition.isAPWC && run.competition.type == type ){
                runs++;
            }
        }
        return runs;
    }

    def  double addCompensation(Competitor competitor){
        def points = 0;
        Competition.findByIsAPWCAndEndTimeIsNotNull(true).each {competition->
            points += (competition as Competition).computeCompetitorCompensation(competitor)

        }

        return points;
    }




}
