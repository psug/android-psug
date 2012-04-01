package fr.psug.android

import android.app.Activity
import android.content.Context
import android.os.{Bundle, Handler}
import io.Source
import org.apache.http.client.methods.HttpGet
import org.apache.http.HttpStatus
import org.apache.http.impl.client.DefaultHttpClient
import android.location.{Location, LocationManager, LocationListener}
import java.net.URL
import android.graphics.{BitmapFactory, Bitmap}
import android.util.Log
import android.view.View

object Utils {

  implicit def onClickListenerFromFunction (action: View => Unit) = {
    new View.OnClickListener() {
      def onClick(v: View) {
        action( v )
      }
    }
  }

  def asyncTask[T](task: => Option[T])(callback: T => Unit = { _: T => }) {
    val handler = new Handler
    new Thread(new Runnable {
      def run {
        task foreach {
          result =>
            handler post new Runnable {
              def run {
                callback(result)
              }
            }
        }
      }
    }) start

  }



  def httpRequest[T](url: String, parser: String => T)(block: T => Unit) {

    asyncTask {
      val httpclient = new DefaultHttpClient()
      val response = httpclient.execute(new HttpGet(url))

      response.getStatusLine.getStatusCode match {
        case HttpStatus.SC_OK =>
          Some(Source.fromInputStream(response.getEntity.getContent).mkString(""))

        case _ => None
      }
    } {
      result =>
        block(parser(result))
    }
  }


  def loadBitmap( url:URL ):Option[Bitmap] = {
    try{
      val connection = url.openConnection()
      connection.setConnectTimeout( 5*1000 )
      connection.setReadTimeout( 5*1000 )

      val bitmap = BitmapFactory.decodeStream(connection.getInputStream)
      Some( bitmap )
    } catch {
      case e =>
        Log.e( "Load bitmap", e.getMessage, e )
        None
    }
  }

  def onLocationChanged(block: Location => Unit)(implicit activity: Activity) {
    val locationManager = activity.getSystemService(Context.LOCATION_SERVICE).asInstanceOf[LocationManager]
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10 * 1000, 0, new LocationListener {
      override def onLocationChanged(loc: Location) {
        block(loc)
      }

      override def onStatusChanged(provider: String, status: Int, extras: Bundle) {
      }

      override def onProviderEnabled(provider: String) {
      }

      override def onProviderDisabled(provider: String) {
      }

    })
  }



}
