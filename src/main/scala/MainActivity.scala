package fr.psug.android

import _root_.android.app.Activity
import org.json.JSONObject
import android.os.{Bundle}
import fr.psug.android.Twitter.Twit
import android.widget.ArrayAdapter
import android.content.Context
import android.view.{Window, ViewGroup, View}
import java.net.URL
import android.util.Log


object Twitter{
  case class Twit( name:String, text:String, profileImageURL:String )

  def jsonSearchResponseParser( text:String ) = {
    val env = new JSONObject( text )
    val jsonArray = env.getJSONArray( "results" )
    val resultJsons = (0 until jsonArray.length() ).map{ jsonArray.getJSONObject( _ ) }

    resultJsons.map{ jsonObj =>
      Twit( jsonObj.getString("from_user"), jsonObj.getString("text"), jsonObj.getString("profile_image_url"))
    }

  }
}



class MainActivity extends Activity with TypedActivity {
  implicit val activity = this

  import Utils._


  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.main)

    var twitterRequester:Option[ (Seq[Twit] => Unit) => Unit ] = None
    setProgressBarIndeterminateVisibility( true )

    onLocationChanged{ loc =>
      val twitterQuery = "http://search.twitter.com/search.json?q=&geocode=%f,%f,%fkm" format( loc.getLatitude, loc.getLongitude, 5.0 )
      twitterRequester = Some( httpRequest( twitterQuery, Twitter.jsonSearchResponseParser ) _ )
      refreshList(twitterRequester)
    }


    findView(TR.refresh_button).setOnClickListener{ view:View =>
      setProgressBarIndeterminateVisibility( true )
      refreshList( twitterRequester )
    }



  }


  def refreshList( requester:Option[ (Seq[Twit] => Unit) => Unit ] ) {
    requester.foreach( _ { result =>
      setProgressBarIndeterminateVisibility( false )

      val twitsView  = findView(TR.twit_list)
      twitsView.setAdapter( new TwitAdapter( R.layout.twit_row, result ) )

    })
  }



  class TwitAdapter( resourceId:Int, twits:Seq[Twit])(implicit context:Context)
    extends ArrayAdapter[Twit]( context, resourceId, twits.toArray ) {


    override def getView( position:Int, convertView:View, parent:ViewGroup  ):View = {
      val view =  if( convertView != null ) convertView
                  else getLayoutInflater.inflate( R.layout.twit_row, parent, false )
      val v = TypedResource.view2typed( view )

      val twit = getItem( position )

      val nameTextView = v.findView(TR.row_name)
      nameTextView.setText( twit.name )

      val textTextView = v.findView(TR.row_text)
      textTextView.setText( twit.text )

      val image = v.findView(TR.row_image)
      image.setImageBitmap( null )
      asyncTask{
        loadBitmap( new URL( twit.profileImageURL ) )
      }{ bitmap =>
        image.setImageBitmap( bitmap )
      }


      view


    }
  }

}

