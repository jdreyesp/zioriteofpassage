package com.rockthejvm.reviewboard.services

import zio._
import java.util.Properties
import javax.mail.Session
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.internet.MimeMessage
import javax.mail.Message
import javax.mail.Transport
import com.rockthejvm.reviewboard.config.EmailServiceConfig
import com.rockthejvm.reviewboard.config.Configs

trait EmailService {
  def sendEmail(to: String, subject: String, content: String): Task[Unit]
  def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit]
}

class EmailServiceLive private (config: EmailServiceConfig) extends EmailService {

  // We're using Ethereal, which is a fake SMTP server: https://ethereal.email/
  private val host: String = config.host
  private val port: Int    = config.port.toInt
  private val user: String = config.user
  private val pass: String = config.pass

  override def sendEmail(to: String, subject: String, content: String): Task[Unit] = {
    val messageZIO = for {
      prop    <- propsResource
      session <- createSession(prop)
      message <- createMessage(session)("daniel@rockthejvm.com", to, subject, content)
    } yield message

    messageZIO.map(message => Transport.send(message))
  }

  override def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] = {
    val subject = "Rock the JVM: Password Recovery"
    val content = s"""
      <div style="
        border: 1px solid black;
        padding: 20px;
        font-family: sans-serif;
        line-height: 2;
        font-size: 20px;
      ">
      <div>
        <h1>Rock the JVM: Password Recovery</h1>
        <p>Your password recovery token is: <strong>$token</strong></p>
        <p>😘 from Rock the JVM</p>
      </div>
    """

    sendEmail(to, subject, content)
  }

  private val propsResource: Task[Properties] = {
    ZIO.succeed {
      val prop = new Properties()
      prop.put("mail.smtp.auth", true)
      prop.put("mail.smtp.starttls.enable", "true")
      prop.put("mail.smtp.host", host)
      prop.put("mail.smtp.port", port)
      prop.put("mail.smtp.ssl.trust", host)
      prop
    }
  }

  private def createSession(prop: Properties): Task[Session] = ZIO.attempt {
    Session.getInstance(
      prop,
      new Authenticator {
        override protected def getPasswordAuthentication(): PasswordAuthentication =
          new PasswordAuthentication(user, pass)
      }
    )
  }

  private def createMessage(
      session: Session
  )(from: String, to: String, subject: String, content: String): Task[MimeMessage] = {
    ZIO.succeed {
      val message = new MimeMessage(session)
      message.setFrom(from)
      message.setRecipients(Message.RecipientType.TO, to)
      message.setSubject(subject)
      message.setContent(content, "text/html; charset=utf-8")
      message
    }
  }
}

object EmailServiceLive {
  val layer = ZLayer(ZIO.service[EmailServiceConfig].map(new EmailServiceLive(_)))

  val configuredLayer =
    Configs.makeConfigLayer[EmailServiceConfig]("rockthejvm.email") >>> layer
}

// go to Ethereal and check Messages ;)
object EmailServiceDemo extends ZIOAppDefault {
  val program = for {
    emailService <- ZIO.service[EmailService]
    _            <- emailService.sendPasswordRecoveryEmail("spiderman@rockthejvm.com", "ABCD1234")
    _            <- Console.printLine("Email done.")
  } yield ()

  def run = program.provide(EmailServiceLive.configuredLayer)
}
