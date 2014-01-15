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

import java.text.DecimalFormat
import java.math.RoundingMode
import org.grails.plugins.csv.CSVWriter



class ResultCompetitionController {

    def index = {
        def activeCompetition = Competition.findByStartTimeIsNotNullAndEndTimeIsNull()
        if (activeCompetition) {
            redirect(action: "show", id: activeCompetition.id)
        }
        else {
            flash.message = "No active competition"
            render(view: 'show');
        }
    }

    def show = {
        def competitionInstance = Competition.get(params.id)
        def isSolo = (competitionInstance.type == Competition.Type.Solo)
        return [competitionInstance: competitionInstance, isSolo: isSolo, competitionId: competitionInstance.id,  endedRuns: competitionInstance.findStartedRuns(), competitorResults: competitionInstance.computeResults()]
    }

    def reportCompetitionResults = {

        def competitionInstance = Competition.get(params.competition_id)

        def competitionResults = competitionInstance.computeResults()

        def runs = competitionInstance.findStartedRuns()

        def labels = [:]
        def fields = []
        def  resultList = []
        competitionResults.eachWithIndex{ result, i ->
            //add rank
            def expanded_record = ["rank": i+1]
            if (!fields.contains("rank")){
                fields.add("rank")
                labels["rank"] = "Rank"
            }
            //Add Competitor
            expanded_record["competitor"] =  result.competitor
            if (!fields.contains("competitor")){
                fields.add("competitor")
                labels["competitor"] = "Competitor"
            }

            //Add warnings
            expanded_record["Warnings"] =  (int)result.warnings
            if (!fields.contains("Warnings")){
                fields.add("Warnings")
                labels["Warnings"] = "Warnings"
            }

            if (result.competitor instanceof Pilot){
                def flight_pilot = result.competitor as Pilot
                def pilot = Pilot.get(flight_pilot.id)
                expanded_record["Country"] =  pilot.country
                if (!fields.contains("Country")){
                    fields.add("Country")
                    labels["Country"] = "Country"
                }

                expanded_record["Glider"] =  pilot.glider
                if (!fields.contains("Glider")){
                    fields.add("Glider")
                    labels["Glider"] = "Glider"
                }
            }
            else{
                def team= result.competitor as Team
                def pilot = team.pilots.asList().get(0);
                def pilot1 = team.pilots.asList().get(1);


                expanded_record["Pilot"] =  pilot.name
                if (!fields.contains("Pilot")){
                    fields.add("Pilot")
                    labels["Pilot"] = "Pilot"
                }

                expanded_record["Country"] =  pilot.country
                if (!fields.contains("Country")){
                    fields.add("Country")
                    labels["Country"] = "Country"
                }

                expanded_record["Glider"] =  pilot.glider
                if (!fields.contains("Glider")){
                    fields.add("Glider")
                    labels["Glider"] = "Glider"
                }

                expanded_record["Pilot1"] =  pilot1.name
                if (!fields.contains("Pilot1")){
                    fields.add("Pilot1")
                    labels["Pilot1"] = "Pilot1"
                }

                expanded_record["Country1"] =  pilot1.country
                if (!fields.contains("Country1")){
                    fields.add("Country1")
                    labels["Country1"] = "Country1"
                }

                expanded_record["Glider1"] =  pilot1.glider
                if (!fields.contains("Glider1")){
                    fields.add("Glider1")
                    labels["Glider1"] = "Glider1"
                }

            }






            //add overall
            expanded_record["Result"] =  roundMark(result.overall)
            if (!fields.contains("Result")){
                fields.add("Result")
                labels["Result"] = "Result"
            }

            //Add Run results
            runs.eachWithIndex { run, j ->
                def runResult =  result.flights?.get(run.id)
                expanded_record["r" + (j+1).toString()] = (runResult)?roundMark(runResult.result):""
                if (!fields.contains("r" + (j+1).toString())){
                    fields.add("r" + (j+1).toString())
                    labels["r" + j.toString()] = (runResult)?"r" + (j+1).toString():""
                }
                params["ACROPYX_RUN_TITLE_" + (j+1).toString()]   = run.name
            }


            resultList.add expanded_record
        }


        params.ACROPYX_COMPETITION = competitionInstance.name
        params.ACROPYX_RESULT = (competitionInstance.isEnded())? "Final overall ranking": "Intermediate overall results"
        chain(controller:'jasper',action:'index',model:[data:resultList],params:params)

    }

    def reportCompetitionResultsCompensation = {

        def competitionInstance = Competition.get(params.competition_id)

        def competitionResults = competitionInstance.computeResults()

        def runs = competitionInstance.findStartedRuns()

        def labels = [:]
        def fields = []
        def  resultList = []
        def hasCompensations = false
        competitionResults.eachWithIndex{ result, i ->
            //add rank
            def expanded_record = ["rank": i+1]
            if (!fields.contains("rank")){
                fields.add("rank")
                labels["rank"] = "Rank"
            }
            //Add Competitor
            expanded_record["competitor"] =  result.competitor
            if (!fields.contains("competitor")){
                fields.add("competitor")
                labels["competitor"] = "Competitor"
            }

            //Add warnings
            expanded_record["Warnings"] =  (int)result.warnings
            if (!fields.contains("Warnings")){
                fields.add("Warnings")
                labels["Warnings"] = "Warnings"
            }

            if (result.competitor instanceof Pilot){
                def flight_pilot = result.competitor as Pilot
                def pilot = Pilot.get(flight_pilot.id)
                expanded_record["Country"] =  pilot.country
                if (!fields.contains("Country")){
                    fields.add("Country")
                    labels["Country"] = "Country"
                }

                expanded_record["Glider"] =  pilot.glider
                if (!fields.contains("Glider")){
                    fields.add("Glider")
                    labels["Glider"] = "Glider"
                }
            }
            else{
                def team= result.competitor as Team
                def pilot = team.pilots.asList().get(0);
                def pilot1 = team.pilots.asList().get(1);


                expanded_record["Pilot"] =  pilot.name
                if (!fields.contains("Pilot")){
                    fields.add("Pilot")
                    labels["Pilot"] = "Pilot"
                }

                expanded_record["Country"] =  pilot.country
                if (!fields.contains("Country")){
                    fields.add("Country")
                    labels["Country"] = "Country"
                }

                expanded_record["Glider"] =  pilot.glider
                if (!fields.contains("Glider")){
                    fields.add("Glider")
                    labels["Glider"] = "Glider"
                }

                expanded_record["Pilot1"] =  pilot1.name
                if (!fields.contains("Pilot1")){
                    fields.add("Pilot1")
                    labels["Pilot1"] = "Pilot1"
                }

                expanded_record["Country1"] =  pilot1.country
                if (!fields.contains("Country1")){
                    fields.add("Country1")
                    labels["Country1"] = "Country1"
                }

                expanded_record["Glider1"] =  pilot1.glider
                if (!fields.contains("Glider1")){
                    fields.add("Glider1")
                    labels["Glider1"] = "Glider1"
                }

            }


            //add overall
            expanded_record["Result"] =  roundMark(competitionInstance.computeCompetitorResultWithCompensation(result.competitor as Competitor))  //roundMark(result.overall)
            if (!fields.contains("Result")){
                fields.add("Result")
                labels["Result"] = "Result"
            }

            //Add Run results
            runs.eachWithIndex { run, j ->
                def runResult =  result.flights?.get(run.id)

                def reportResult = 0

                if (runResult && runResult.result > 0){
                    reportResult = runResult.result
                }
                else{
                    if (Flight.findByCompetitorAndRun(result.competitor as Competitor, run) == null){
                             hasCompensations = true
                             reportResult  =  (run as Run).calculateCompetitorCompensation(result.competitor as Competitor )
                             expanded_record["note_r" + (j+1).toString()] =  "*"
                             if (!fields.contains("note_r" + (j+1).toString())){
                                fields.add("note_r" + (j+1).toString())
                                labels["note_r" + j.toString()] = ""

                             }
                    }

                }

                expanded_record["r" + (j+1).toString()] =  roundMark(reportResult)

                if (!fields.contains("r" + (j+1).toString())){
                    fields.add("r" + (j+1).toString())
                    labels["r" + j.toString()] = (runResult)?"r" + (j+1).toString():""
                }
                params["ACROPYX_RUN_TITLE_" + (j+1).toString()]   = run.name
                if (hasCompensations){
                    params["ACROPYX_COMPENSATION_NOTE"] = "* Cut compensation points"
                }
            }


            resultList.add expanded_record
        }


        params.ACROPYX_COMPETITION = competitionInstance.name
        params.ACROPYX_RESULT = (competitionInstance.isEnded())? "Final overall ranking": "Intermediate overall results"
        chain(controller:'jasper',action:'index',model:[data:resultList],params:params)

    }



    def exportCompetition = {
        def competitionInstance = Competition.get(params.id)

        def runs = competitionInstance.findStartedRuns()

        def sw = new StringWriter()
        def csv = new CSVWriter(sw, {
           Rank { it.rank }
           CivlId { it.civlId }
           Competitor { it.competitor }
           Country {it.country}
           Glider {it.glider}
           Warnings {it.warnings}
           runs.each { run ->
                "${run.name}" {it."${run.name}"}
            }
            Total {it.overall }
        })


        def competitionResults = competitionInstance.computeResults()





        competitionResults.eachWithIndex{ result, i ->
            //add rank
            def values = [:]
            values.put("rank",  i+1)
            //Add Competitor
             values.put("competitor", result.competitor)
            //Add warnings
            values.put("warnings", (int)result.warnings)

            if (result.competitor instanceof Pilot){
                def flight_pilot = result.competitor as Pilot
                def pilot = Pilot.get(flight_pilot.id)
                values.put("country", pilot.country)
                values.put("glider", pilot.glider)
                values.put("civlId", pilot.civlId)
            }
            else{
              //Add pilots
                def flight_team = result.competitor as Team
                def team = Team.get(flight_team.id)
                def pilots = team.pilots.asList()

                def pilot1 = pilots.get(0)
                def pilot2 = pilots.get(1)

                values.put("competitor", team.name + " ( " + pilot1.name + " / " + pilot2.name + " ) ")
                values.put("country", pilot1.country + " / " + pilot2.country)
                values.put("glider", pilot1.glider + " / " + pilot2.glider)
                values.put("civlId", pilot1.civlId + " / " + pilot2.civlId)

            }
            //add overall
            values.put("overall", roundMark(result.overall))
            //Add Run results
           runs.eachWithIndex { run, j ->
                def runResult =  result.flights?.get(run.id)
                values.put(run.name, (runResult)?roundMark(runResult.result):"")
            }


            csv << values
        }


        response.setContentType("text/csv")
        response.setHeader("Content-disposition", "filename=${competitionInstance}.csv")
        response.outputStream << sw
    }

    def exportCompetitionFAI = {
        def competitionInstance = Competition.get(params.id)

        def runs = competitionInstance.findStartedRuns()

        def sw = new StringWriter()
        def csv = new CSVWriter(sw, {
            Name { it.competitor }
            NAT  {it.country}
            Score {it.overall }
            Sex  { it.sex}
            Birthday  { it.birthday}
            Valid_FAI_licence { it.licenceFAI}
            Glider {it.glider}
            Sponsor  { it.sponsor}
            CIVL_ID { it.civlId }
        })


        def competitionResults = competitionInstance.computeResults()





        competitionResults.eachWithIndex{ result, i ->
            //add rank
            def values = [:]

            if (result.competitor instanceof Pilot){
                def flight_pilot = result.competitor as Pilot
                def pilot = Pilot.get(flight_pilot.id)

                values.put("competitor", result.competitor)
                values.put("country", pilot.country)
                values.put("sex", (pilot.sex == Pilot.Sex.Male)?"0":"1")
                values.put("birthday", pilot.dateOfBirth?pilot.dateOfBirth.format("yyyy-MM-dd"):"")
                values.put("licenceFAI", pilot.licenceFAI?"1":"0")
                values.put("glider", pilot.glider)
                values.put("sponsor", pilot.sponsor)
                values.put("civlId", pilot.civlId?pilot.civlId:"")
                def overall =  competitionInstance.computeCompetitorResultWithCompensation(pilot)
                values.put("overall", roundMark(overall))
                //values.put("overall", roundMark(result.overall))
                csv << values
            }
            else{
                //Add one row peer pilot
                def flight_team = result.competitor as Team
                def team = Team.get(flight_team.id)
                def pilots = team.pilots.asList()


                //first Row

                def pilot1 = pilots.get(0) as Pilot

                values.put("competitor", pilot1.name)
                values.put("country", pilot1.country)
                def sex = ""
                if (pilot1.sex == Pilot.Sex.Male){
                    sex = "0"
                }
                else{
                    sex = "1"
                }
                values.put("sex",sex)
                values.put("birthday", (pilot1.dateOfBirth)?pilot1.dateOfBirth.format("yyyy-MM-dd"):"")

                def licence = ""
                if (pilot1.licenceFAI){
                    licence = "1"
                }
                else{
                    licence = "0"
                }
                values.put("licenceFAI", licence)
                values.put("glider", pilot1.glider)
                values.put("sponsor", "") //team.name)
                values.put("civlId", pilot1.civlId?pilot1.civlId:"")
                def overall =  competitionInstance.computeCompetitorResultWithCompensation(team)
                values.put("overall", roundMark(overall))
                //values.put("overall", roundMark(result.overall))

                csv << values

                //second row
                values = [:]
                def pilot2 = pilots.get(1) as Pilot
                values.put("competitor", pilot2.name)
                values.put("country", pilot2.country)
                sex = ""
                if (pilot2.sex == Pilot.Sex.Male){
                    sex = "0"
                }
                else{
                    sex = "1"
                }
                values.put("sex",sex)
                values.put("birthday", (pilot2.dateOfBirth)?pilot2.dateOfBirth.format("yyyy-MM-dd"):"")

                licence = ""
                if (pilot2.licenceFAI){
                    licence = "1"
                }
                else{
                    licence = "0"
                }
                values.put("licenceFAI", licence)
                values.put("glider", pilot2.glider)
                values.put("sponsor", "") //team.name)
                values.put("civlId", pilot2.civlId?pilot2.civlId:"")

                values.put("overall", roundMark(overall))
                //values.put("overall", roundMark(result.overall))

                csv << values
            }


        }


        response.setContentType("text/csv")
        response.setHeader("Content-disposition", "filename=${competitionInstance}.csv")
        response.outputStream << sw
    }

//    def exportCompetitionSync = {
//        def competitionInstance = Competition.get(params.id)
//
//        def runs = competitionInstance.findStartedRuns()
//
//        def sw = new StringWriter()
//        def csv = new CSVWriter(sw, {
//            Rank { it.rank }
//            Team { it.competitor }
//            Pilot1 { it.pilot1 }
//            Pilot2 { it.pilot2 }
//            Warnings {it.warnings}
//            runs.each { run ->
//                "${run.name}" {it."${run.name}"}
//            }
//            Result {it.overall }
//        })
//
//
//        def competitionResults = competitionInstance.computeResults()
//
//        competitionResults.eachWithIndex{ result, i ->
//            //add rank
//            def values = [:]
//            values.put("rank",  i+1)
//            //Add Competitor
//            values.put("competitor", result.competitor)
//            //Add warnings
//            values.put("warnings", (int)result.warnings)
//
//            //Add pilots
//            def flight_team = result.competitor as Team
//            def team = Team.get(flight_team.id)
//            values.put("pilot1", team.pilots[0].name + "(" + team.pilots[0].civlId +")")
//            values.put("pilot2", team.pilots[1].name + "(" + team.pilots[1].civlId +")")
//
//            //add overall
//            values.put("overall", roundMark(result.overall))
//            //Add Run results
//            runs.eachWithIndex { run, j ->
//                def runResult =  result.flights?.get(run.id)
//                values.put(run.name, (runResult)?roundMark(runResult.result):"")
//            }
//
//            csv << values
//        }
//
//
//        response.setContentType("text/csv")
//        response.setHeader("Content-disposition", "filename=${competitionInstance}.csv")
//        response.outputStream << sw
//    }

    def reportCompetitionManoeuvres = {

        def competitionInstance = Competition.get(params.competition_id)

        def competitionResults = competitionInstance.computeManoeuvres()

        def runs = competitionInstance.findStartedRuns()

        def labels = [:]
        def fields = []
        def  resultList = []
        competitionResults.eachWithIndex{ result, i ->
            //add rank
            def expanded_record = ["rank": i+1]
            if (!fields.contains("rank")){
                fields.add("rank")
                labels["rank"] = "Rank"
            }
            //Add Competitor
            expanded_record["competitor"] =  result.competitor
            if (!fields.contains("competitor")){
                fields.add("competitor")
                labels["competitor"] = "Competitor"
            }

            //Add warnings
            expanded_record["Warnings"] =  (int)result.warnings
            if (!fields.contains("Warnings")){
                fields.add("Warnings")
                labels["Warnings"] = "Warnings"
            }

            if (result.competitor instanceof Pilot){
                def flight_pilot = result.competitor as Pilot
                def pilot = Pilot.get(flight_pilot.id)
                expanded_record["Country"] =  pilot.country
                if (!fields.contains("Country")){
                    fields.add("Country")
                    labels["Country"] = "Country"
                }

                expanded_record["Glider"] =  pilot.glider
                if (!fields.contains("Glider")){
                    fields.add("Glider")
                    labels["Glider"] = "Glider"
                }
            }
            else{
                def team= result.competitor as Team
                def pilot = team.pilots.asList().get(0);
                def pilot1 = team.pilots.asList().get(1);


                expanded_record["Pilot"] =  pilot.name
                if (!fields.contains("Pilot")){
                    fields.add("Pilot")
                    labels["Pilot"] = "Pilot"
                }

                expanded_record["Country"] =  pilot.country
                if (!fields.contains("Country")){
                    fields.add("Country")
                    labels["Country"] = "Country"
                }

                expanded_record["Glider"] =  pilot.glider
                if (!fields.contains("Glider")){
                    fields.add("Glider")
                    labels["Glider"] = "Glider"
                }

                expanded_record["Pilot1"] =  pilot1.name
                if (!fields.contains("Pilot1")){
                    fields.add("Pilot1")
                    labels["Pilot1"] = "Pilot1"
                }

                expanded_record["Country1"] =  pilot1.country
                if (!fields.contains("Country1")){
                    fields.add("Country1")
                    labels["Country1"] = "Country1"
                }

                expanded_record["Glider1"] =  pilot1.glider
                if (!fields.contains("Glider1")){
                    fields.add("Glider1")
                    labels["Glider1"] = "Glider1"
                }

            }






            //add overall
            expanded_record["Result"] =  roundMark(result.overall)
            if (!fields.contains("Result")){
                fields.add("Result")
                labels["Result"] = "Result"
            }

            //Add Run results
            runs.eachWithIndex { run, j ->
                def runResult =  result.flights?.get(run.id)
                expanded_record["r" + (j+1).toString()] = (runResult)?roundMark(runResult.result):""
                if (!fields.contains("r" + (j+1).toString())){
                    fields.add("r" + (j+1).toString())
                    labels["r" + j.toString()] = (runResult)?"r" + (j+1).toString():""
                }
                params["ACROPYX_RUN_TITLE_" + (j+1).toString()]   = run.name
            }


            resultList.add expanded_record
        }


        params.ACROPYX_COMPETITION = competitionInstance.name
        params.ACROPYX_RESULT = (competitionInstance.isEnded())? "Final overall ranking": "Intermediate overall results"
        chain(controller:'jasper',action:'index',model:[data:resultList],params:params)

    }

    def reportRankingSolo = {

        def countRun = countRankingRuns(Competition.Type.Solo)
        Map results = computePilotsRanking(countRun)

        def countCompetitions = Competition.countByIsAPWCAndType(true,Competition.Type.Solo)

        def labels = [:]
        def fields = []
        def  resultList = []
        results.eachWithIndex{ result, i ->
            //add rank
            def rank = i+1
            if (i == 1){
                rank = 1
            }
            def expanded_record = ["rank": rank]
            if (!fields.contains("rank")){
                fields.add("rank")
                labels["rank"] = "Rank"
            }
            //Add Competitor
            expanded_record["competitor"] = (result.key as Competitor).name
            if (!fields.contains("competitor")){
                fields.add("competitor")
                labels["competitor"] = "Competitor"
            }

            expanded_record["Country"] = (result.key as Pilot).country
            if (!fields.contains("Country")){
                fields.add("Country")
                labels["Country"] = "Country"
            }

         //   Competition.findAllByIsAPWCAndType(true,Competition.Type.Solo).eachWithIndex{comp, j ->
            def competitions = Competition.listOrderByStartTime()
            def j = 0
            competitions.each{comp ->

                if (comp.isAPWC && comp.type == Competition.Type.Solo){

                    def compResult = (comp as Competition).computeCompetitorResultWithCompensation(result.key as Competitor)
                    expanded_record["r" + (j+1).toString()] = compResult?roundMark(compResult):""
                    if (!fields.contains("r" + (j+1).toString())){
                        fields.add("r" + (j+1).toString())
                        labels["r" + j.toString()] = (compResult)?"r" + (j+1).toString():""
                    }

                    String[] compwords =  comp.name.split()
                    params["ACROPYX_COMPETITION_TITLE_" + (j+1).toString()]   = compwords[0]
                    j++
                }

            }

            //add overall
            expanded_record["Result"] =  roundMark(result.value)  //roundMark(calcResult)
            if (!fields.contains("Result")){
                fields.add("Result")
                labels["Result"] = "Result"
            }


            resultList.add expanded_record
        }


        params.ACROPYX_RANKING = "APWC SOLO 2013"
        params.ACROPYX_RANKING_TITLE = "Final overall ranking" //(competitionInstance.isEnded())? "Final overall ranking": "Intermediate overall results"
        chain(controller:'jasper',action:'index',model:[data:resultList],params:params)

    }

    def reportRankingSync = {
        def countRun = countRankingRuns(Competition.Type.Synchro)
        Map results = computeTeamsRanking(countRun)

        def labels = [:]
        def fields = []
        def  resultList = []
        results.eachWithIndex{ result, i ->
            //add rank
            def expanded_record = ["rank": i+1]
            if (!fields.contains("rank")){
                fields.add("rank")
                labels["rank"] = "Rank"
            }
            //Add Competitor
            expanded_record["competitor"] = (result.key as Competitor).name
            if (!fields.contains("competitor")){
                fields.add("competitor")
                labels["competitor"] = "Competitor"
            }

            def team= result.key as Team
            def pilot = team.pilots.asList().get(0);
            def pilot1 = team.pilots.asList().get(1);


            expanded_record["Pilot"] =  pilot.name
            if (!fields.contains("Pilot")){
                fields.add("Pilot")
                labels["Pilot"] = "Pilot"
            }

            expanded_record["Country"] =  pilot.country
            if (!fields.contains("Country")){
                fields.add("Country")
                labels["Country"] = "Country"
            }

            expanded_record["Pilot1"] =  pilot1.name
            if (!fields.contains("Pilot1")){
                fields.add("Pilot1")
                labels["Pilot1"] = "Pilot1"
            }

            expanded_record["Country1"] =  pilot1.country
            if (!fields.contains("Country1")){
                fields.add("Country1")
                labels["Country1"] = "Country1"
            }


            //   Competition.findAllByIsAPWCAndType(true,Competition.Type.Solo).eachWithIndex{comp, j ->
            def competitions = Competition.listOrderByStartTime()
            def j = 0
            competitions.each{comp ->

                if (comp.isAPWC && comp.type == Competition.Type.Synchro){
                    def compResult = (comp as Competition).computeCompetitorResultWithCompensation(result.key as Competitor)
                    expanded_record["r" + (j+1).toString()] = compResult?roundMark(compResult):""
                    if (!fields.contains("r" + (j+1).toString())){
                        fields.add("r" + (j+1).toString())
                        labels["r" + j.toString()] = (compResult)?"r" + (j+1).toString():""
                    }
                    String[] compwords =  comp.name.split()
                    params["ACROPYX_COMPETITION_TITLE_" + (j+1).toString()]   = compwords[0]
                    j++
                }
            }



            //add overall
            expanded_record["Result"] =  roundMark(result.value)
            if (!fields.contains("Result")){
                fields.add("Result")
                labels["Result"] = "Result"
            }


            resultList.add expanded_record
        }


        params.ACROPYX_RANKING = "APWC SYNC 2013"
        params.ACROPYX_RANKING_TITLE = "Final overall ranking" //(competitionInstance.isEnded())? "Final overall ranking": "Intermediate overall results"
        chain(controller:'jasper',action:'index',model:[data:resultList],params:params)

    }

    //RANKING - MOVE TO COMMON SERVICE


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
//            def runs = 0
//            Flight.findAllByCompetitorAndEndTimeIsNotNull(pilot).each {flight ->
//                if (flight.run.isEnded()  && flight.run.competition.isAPWC){
//                    points += flight.computeFlightResult(false)
//                    runs++
//                }
//            }
//
//            points += addCompensation(pilot)

            Competition.findAllByIsAPWCAndEndTimeIsNotNull(true).each { comp ->
                if (comp.type == Competition.Type.Solo ){
                    points += comp.computeCompetitorResultWithCompensation(pilot)
                }
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
//            def points = 0
//            def runs = 0
//            Flight.findAllByCompetitorAndEndTimeIsNotNull(team).each {flight ->
//                if (flight.run.isEnded() && flight.run.competition.isAPWC){
//                    points += flight.computeFlightResult(false)
//                    runs++
//                }
//            }
//
//            if (runs < countRuns){
//                points += addCompensation(team)
//            }
            def points = 0
            Competition.findAllByIsAPWCAndEndTimeIsNotNull(true).each { comp ->
                if (comp.type == Competition.Type.Synchro ){
                    points += comp.computeCompetitorResultWithCompensation(team)
                }
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


    //END RANKING

    def  roundMark(mark) {
        def decimalFormat = new DecimalFormat("0.000")
        decimalFormat.setRoundingMode(RoundingMode.HALF_UP)
        return decimalFormat.format(mark)
        //return markString as double
    }
}
