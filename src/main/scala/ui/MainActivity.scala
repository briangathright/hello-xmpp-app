package edu.luc.etl.cs313.scala.hello.xmpp
package ui

import java.util.{UUID, ArrayList}

import android.widget.ArrayAdapter
import android.app.Activity
import android.os.{AsyncTask, Bundle}
import android.util.Log
import android.view.View
import org.jivesoftware.smack.packet.Message
import org.jivesoftware.smack.tcp.{XMPPTCPConnectionConfiguration, XMPPTCPConnection}
import org.jivesoftware.smack.{AbstractXMPPConnection, ConnectionConfiguration}
import org.jivesoftware.smack._
import org.jivesoftware.smackx.muc.{InvitationListener, RoomInfo, MultiUserChat, MultiUserChatManager}
import org.jivesoftware.smackx.xdata.Form
import org.jxmpp.util.XmppStringUtils

/**
 * The main Android activity, which provides the required lifecycle methods.
 * By mixing in the reactive view behaviors, this class serves as the Adapter
 * in the Model-View-Adapter pattern. It connects the Android GUI view with the
 * reactive model.
 */
class MainActivity extends Activity with TypedActivity {

  val HOST = "talk.google.com"
  val PORT = 5222
  val SERVICE = "gmail.com"

  var username: String = _
  var password: String = _
  var isMUC: Boolean = _
  var isHost: Boolean = _

  var nickname: String = _
  private var chatmanager: ChatManager = _
  private var mucmanager: MultiUserChatManager = _

  var muc: MultiUserChat = _
  var chat2: Chat = _
  var connection: AbstractXMPPConnection = _

  private val messages = new ArrayList[String]
  private val TAG = "xmpp-android-activity" // FIXME please use this in all log messages

  private def usernameET = findView(TR.usernameET)

  private def passwordET = findView(TR.passwordET)

  private def checkBox = findView(TR.hostCheckBox)

  private def mucCheckBox = findView(TR.mucCheckBox)

  private def recipient = findView(TR.recipientET)

  private def inviteET = findView(TR.inviteET)

  private def send = findView(TR.sendButton)

  private def mucSend = findView(TR.mucSendButton)

  private def textMessage = findView(TR.chatBoxET)

  private def listview = findView(TR.listMessages)

  private def mucTextMessage = findView(TR.mucChatBoxET)

  private def mucListview = findView(TR.mucListMessages)


  override def onCreate(savedInstanceState: Bundle) = {
    super.onCreate(savedInstanceState)
    Log.d(TAG, "onCreate")
    // inject the (implicit) dependency on the view
    setContentView(R.layout.login)
  }

  override def onStart() = {
    super.onStart()
    Log.d(TAG, "onStart")
  }

  def onLogin(view: View): Unit = {
    username = usernameET.getText.toString
    password = passwordET.getText.toString
    isHost = checkBox.isChecked
    isMUC = mucCheckBox.isChecked
    Log.d(TAG, "user = " + username + ", pw = " + password)
    connect()

    if (isMUC) {
      setContentView(R.layout.muc)
    }
    else {
      setContentView(R.layout.main)
    }

    setListAdapter()
  }

  private def setListAdapter(): Unit = {
    val adapter = new ArrayAdapter[String](this, R.layout.listitem, messages)
    if (isMUC) {
      mucListview.setAdapter(adapter)
    }
    else {
      listview.setAdapter(adapter)
    }
  }

  private def runOnBackgroundThread(task: => Unit): Unit =
    AsyncTask.execute(new Runnable {
      override def run() = task
    })

  private def runOnUiThread(task: => Unit): Unit =
    runOnUiThread(new Runnable {
      override def run() = task
    })

  override def onDestroy() = {
    super.onDestroy()
    runOnBackgroundThread {
      connection.disconnect()
    }
  }

  def createMUC(): Unit = {
    Log.d(TAG, "getting MultiUserChatManager")
    mucmanager = MultiUserChatManager.getInstanceFor(connection)
    Log.d(TAG, "got MultiUserChatManager")
    if (isHost) {
      Log.d(TAG, username + " is hosting a multiuserchat")
      val uid: UUID = UUID.randomUUID
      val chatRoomName = String.format("private-chat-%1s@%2s", uid, "groupchat.google.com")
      //val chatRoomName: String = "lucawall@groupchat.google.com"
      muc = mucmanager.getMultiUserChat(chatRoomName)
      try {
        muc.join(username)
        Log.d(TAG, "Creating MultiUserChat room with name: " + chatRoomName)
        muc.sendConfigurationForm(new Form(Form.TYPE_SUBMIT))
      } catch {
        case ex: SmackException =>
          Log.e(TAG, ex.toString)
      }
      muc.addMessageListener(new MessageListener {
        override def processMessage(message: Message): Unit = {
          if (message.getBody != null) {
            val from = XmppStringUtils.parseResource(message.getFrom)
            runOnUiThread {
              if (from.equals(username)) {
                messages.add("You: " + message.getBody)
              }
              else {
                messages.add(from + ": " + message.getBody)
              }
              setListAdapter()
            }
          }
        }
      })
    }
    else {
      Log.d(TAG, username + " is waiting to join a multiuserchat")
      mucmanager.addInvitationListener(new InvitationListener {
        override def invitationReceived(conn: XMPPConnection, room: String, inviter: String, reason: String, password: String, message: Message): Unit = {
          muc = mucmanager.getMultiUserChat(room)
          muc.join(username, password)
          muc.addMessageListener(new MessageListener {
            override def processMessage(message: Message): Unit = {
              if (message.getBody != null) {
                val from = XmppStringUtils.parseResource(message.getFrom)
                runOnUiThread {
                  if (from.equals(username)) {
                    messages.add("You: " + message.getBody)
                  }
                  else {
                    messages.add(from + ": " + message.getBody)
                  }
                  setListAdapter()
                }
              }
            }
          })
        }
      })
    }
  }


  def connect(): Unit = {
    runOnBackgroundThread {
      Log.d(TAG, "Getting config...")
      val configBuilder = XMPPTCPConnectionConfiguration.builder()
      configBuilder.setUsernameAndPassword(username, password)
      configBuilder.setHost(HOST)
      configBuilder.setPort(PORT)
      configBuilder.setServiceName(SERVICE)
      Log.d(TAG, "Getting conn...")
      connection = new XMPPTCPConnection(configBuilder.build())
      try {
        Log.d(TAG, "Connectings...")
        connection.connect()
        Log.d(TAG, "Connection succeded")
      } catch {
        case ex: XMPPException =>
          Log.e(TAG, "Failed to connect to " + connection.getHost)
          Log.e(TAG, ex.toString)
      }
      try {
        connection.login()
        Log.d(TAG, "logged in successfully")
      } catch {
        case ex: XMPPException =>
          Log.e(TAG, "Failed to log in as " + username)
          Log.e(TAG, ex.toString)
      }
      chatmanager = ChatManager.getInstanceFor(connection)
      Log.d(TAG, "got chat manager")
      chatmanager.addChatListener(new ChatManagerListener {
        override def chatCreated(chat: Chat, createdLocally: Boolean) = {
          if (chat2 == null) chat2 = chat
          if (!createdLocally) {
            val from = chat2.getParticipant
            chat2.addMessageListener(new ChatMessageListener {
              override def processMessage(chat: Chat, message: Message) = {
                if (message.getBody != null) {
                  runOnUiThread {
                    messages.add(from + ": " + message.getBody)
                    setListAdapter()
                  }
                }
              }
            })
          }
        }
      })
      Log.d(TAG, "attached chat listener")

      createMUC()
    }
  }

  def onSend(view: View): Unit = {
    val to = recipient.getText.toString
    val text = textMessage.getText.toString
    Log.d(TAG, "Sending text " + text + " to " + to)
    if (chat2 == null) {
      chat2 = chatmanager.createChat(to, new ChatMessageListener {
        override def processMessage(chat: Chat, message: Message) = {
          if (message.getBody != null) {
            val from = chat.getParticipant
            runOnUiThread {
              messages.add(from + ": " + message.getBody)
              setListAdapter()
            }
          }
        }
      })
    }
    val msg = new Message(to, Message.Type.chat)
    msg.setBody(text)
    if (connection != null) {
      connection.sendPacket(msg)
      messages.add("You: " + text)
      setListAdapter()
    }
    textMessage.setText("")
  }


  def onSendMUC(view: View): Unit = {
    val text = mucTextMessage.getText.toString
    if (text.equals("exit") && isHost) {
      muc.leave()
      muc.destroy("done...", "none")
      Log.d(TAG, "Destroyed Room")
    }
    else {
      Log.d(TAG, "Sending text " + text)
      setListAdapter()
      muc.sendMessage(text)
      mucTextMessage.setText("")
    }
  }

  def onInviteMUC(view: View): Unit = {
    val invitee = inviteET.getText.toString
    Log.d(TAG, "Inviting " + invitee + " to the room")
    muc.invite(invitee, "come join me in this room")
    inviteET.setText("")
  }
}

