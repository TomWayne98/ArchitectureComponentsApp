package cz.pepa.runapp.logic

import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataDeleteRequest
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResult
import cz.pepa.runapp.app
import cz.pepa.runapp.data.DummyFittnes
import io.stepuplabs.settleup.util.ld
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * TODO: Add description
 *
 * @author Josef Hruška (josef@stepuplabs.io)
 */

object Fit {

    var mClient: GoogleApiClient? = null
    private val REQUEST_OAUTH = 1
    private val DATE_FORMAT = "yyyy.MM.dd HH:mm:ss"

    fun getDummyFitnessData(): List<DummyFittnes> {
        val one = DummyFittnes("Marcel", "Running", 600)
        val two = DummyFittnes("Kuba", "Running", 730)
        val three = DummyFittnes("Pepa", "Running", 350)

        val dummies = listOf<DummyFittnes>(one, two, three)

        return dummies
    }

    /**
     * Build a [GoogleApiClient] that will authenticate the user and allow the application
     * to connect to Fitness APIs. The scopes included should match the scopes your app needs
     * (see documentation for details). Authentication will occasionally fail intentionally,
     * and in those cases, there will be a known resolution, which the OnConnectionFailedListener()
     * can address. Examples of this include the user never having signed in before, or
     * having multiple accounts on the device and needing to specify which account to use, etc.
     */
    fun buildFitnessClient(activity: FragmentActivity) {
        // Create the Google API Client
        mClient = GoogleApiClient.Builder(app())
                .addApi(Fitness.HISTORY_API)
                .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addConnectionCallbacks(
                        object : GoogleApiClient.ConnectionCallbacks {
                            override fun onConnected(bundle: Bundle?) {
                                ld( "Connected!!!")
                                // Now you can make calls to the Fitness APIs.  What to do?
                                // Look at some data!!
                                InsertAndVerifyDataTask().execute()
                            }

                            override fun onConnectionSuspended(i: Int) {
                                // If your connection to the sensor gets lost at some point,
                                // you'll be able to determine the reason and react to it here.
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    ld("Connection lost.  Cause: Network Lost.")
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    ld("Connection lost.  Reason: Service Disconnected")
                                }
                            }
                        }
                )
                .enableAutoManage(activity, 0) { result ->

                }
                .build()
    }

    /**
     * Create a [DataSet] to insert data into the History API, and
     * then create and execute a [DataReadRequest] to verify the insertion succeeded.
     * By using an [AsyncTask], we can schedule synchronous calls, so that we can query for
     * data after confirming that our insert was successful. Using asynchronous calls and callbacks
     * would not guarantee that the insertion had concluded before the read request was made.
     * An example of an asynchronous call using a callback can be found in the example
     * on deleting data below.
     */
    private class InsertAndVerifyDataTask : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void): Void? {
            // Create a new dataset and insertion request.
//            val dataSet = insertFitnessData()

            // [START insert_dataset]
            // Then, invoke the History API to insert the data and await the result, which is
            // possible here because of the {@link AsyncTask}. Always include a timeout when calling
            // await() to prevent hanging that can occur from the service being shutdown because
            // of low memory or other conditions.
//            ld( "Inserting the dataset in the History API.")
//            val insertStatus = Fitness.HistoryApi.insertData(mClient, dataSet)
//                    .await(1, TimeUnit.MINUTES)
//
//            // Before querying the data, check to see if the insertion succeeded.
//            if (!insertStatus.isSuccess) {
//                ld( "There was a problem inserting the dataset.")
//                return null
//            }

            // At this point, the data has been inserted and can be read.
            ld( "Data insert was successful!")
            // [END insert_dataset]

            // Begin by creating the query.
            val readRequest = queryFitnessData()

            // [START read_dataset]
            // Invoke the History API to fetch the data with the query and await the result of
            // the read request.
            val dataReadResult = Fitness.HistoryApi.readData(mClient, DataType.TYPE_STEP_COUNT_DELTA).await(30, TimeUnit.SECONDS)
            // [END read_dataset]

            // For the sake of the sample, we'll print the data so we can see what we just added.
            // In general, logging fitness information should be avoided for privacy reasons.
//            printData(dataReadResult.total)
            ld("Size of data is ${dataReadResult.total?.toString()}")
            return null
        }
    }

    /**
     * Create and return a [DataSet] of step count data for insertion using the History API.
     */
    private fun insertFitnessData(): DataSet {
        ld( "Creating a new data insert request.")

        // [START build_insert_data_request]
        // Set a start and end time for our data, using a start time of 1 hour before this moment.
        val cal = Calendar.getInstance()
        val now = Date()
        cal.time = now
        val endTime = cal.timeInMillis
        cal.add(Calendar.HOUR_OF_DAY, -1)
        val startTime = cal.timeInMillis

        // Create a data source
        val dataSource = DataSource.Builder()
                .setAppPackageName(app().packageName) // TODO: Maybe replace?
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setStreamName(" - step count")
                .setType(DataSource.TYPE_RAW)
                .build()

        // Create a data set
        val stepCountDelta = 950
        val dataSet = DataSet.create(dataSource)
        // For each data point, specify a start time, end time, and the data value -- in this case,
        // the number of new steps.
        val dataPoint = dataSet.createDataPoint()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
        dataPoint.getValue(Field.FIELD_STEPS).setInt(stepCountDelta)
        dataSet.add(dataPoint)
        // [END build_insert_data_request]

        return dataSet
    }

    /**
     * Return a [DataReadRequest] for all step count changes in the past week.
     */
    fun queryFitnessData(): DataReadRequest {
        // [START build_read_data_request]
        // Setting a start and end date using a range of 1 week before this moment.
        val cal = Calendar.getInstance()
        val now = Date()
        cal.time = now
        val endTime = cal.timeInMillis
        cal.add(Calendar.MONTH, -1)
        val startTime = cal.timeInMillis

        val dateFormat = DateFormat.getDateInstance()
        ld( "Range Start: " + dateFormat.format(startTime))
        ld( "Range End: " + dateFormat.format(endTime))

        val readRequest = DataReadRequest.Builder()
                // The data request can specify multiple data types to return, effectively
                // combining multiple data queries into one call.
                // In this example, it's very unlikely that the request is for several hundred
                // datapoints each consisting of a few steps and a timestamp.  The more likely
                // scenario is wanting to see how many steps were walked per day, for 7 days.
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                // Analogous to a "Group By" in SQL, defines how data should be aggregated.
                // bucketByTime allows for a time span, whereas bucketBySession would allow
                // bucketing by "sessions", which would need to be defined in code.
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build()
        // [END build_read_data_request]

        return readRequest
    }

    /**
     * Log a record of the query result. It's possible to get more constrained data sets by
     * specifying a data source or data type, but for demonstrative purposes here's how one would
     * dump all the data. In this sample, logging also prints to the device screen, so we can see
     * what the query returns, but your app should not log fitness information as a privacy
     * consideration. A better option would be to dump the data you receive to a local data
     * directory to avoid exposing it to other applications.
     */
    fun printData(dataReadResult: DataReadResult) {
        // [START parse_read_data_result]
        // If the DataReadRequest object specified aggregated data, dataReadResult will be returned
        // as buckets containing DataSets, instead of just DataSets.
        if (dataReadResult.buckets.size > 0) {
            ld( "Number of returned buckets of DataSets is: " + dataReadResult.buckets.size)
            for (bucket in dataReadResult.buckets) {
                val dataSets = bucket.dataSets
                for (dataSet in dataSets) {
                    dumpDataSet(dataSet)
                }
            }
        } else if (dataReadResult.dataSets.size > 0) {
            ld( "Number of returned DataSets is: " + dataReadResult.dataSets.size)
            for (dataSet in dataReadResult.dataSets) {
                dumpDataSet(dataSet)
            }
        }
        // [END parse_read_data_result]
    }

    // [START parse_dataset]
    private fun dumpDataSet(dataSet: DataSet) {
        ld( "Data returned for Data type: " + dataSet.dataType.name)
        val dateFormat = DateFormat.getTimeInstance()

        for (dp in dataSet.dataPoints) {
            ld( "Data point:")
            ld( "\tType: " + dp.dataType.name)
            ld( "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)))
            ld( "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)))
            for (field in dp.dataType.fields) {
                ld( "\tField: " + field.name +
                        " Value: " + dp.getValue(field))
            }
        }
    }
    // [END parse_dataset]

    /**
     * Delete a [DataSet] from the History API. In this example, we delete all
     * step count data for the past 24 hours.
     */
    private fun deleteData() {
        ld( "Deleting today's step count data.")

        // [START delete_dataset]
        // Set a start and end time for our data, using a start time of 1 day before this moment.
        val cal = Calendar.getInstance()
        val now = Date()
        cal.time = now
        val endTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = cal.timeInMillis

        //  Create a delete request object, providing a data type and a time interval
        val request = DataDeleteRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .build()

        // Invoke the History API with the Google API client object and delete request, and then
        // specify a callback that will check the result.
        Fitness.HistoryApi.deleteData(mClient, request)
                .setResultCallback { status ->
                    if (status.isSuccess) {
                        ld( "Successfully deleted today's step count data.")
                    } else {
                        // The deletion will fail if the requesting app tries to delete data
                        // that it did not insert.
                        ld( "Failed to delete today's step count data.")
                    }
                }
        // [END delete_dataset]
    }

}