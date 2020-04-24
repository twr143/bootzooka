package template.util
import java.security.MessageDigest


/**
  * Created by Ilya Volynin on 23.04.2020 at 20:40.
  */
object Hashing {

  private lazy val messageDigest: MessageDigest = MessageDigest.getInstance("SHA-512")

  def digest(s: String): String = {
    messageDigest.update((s + "tr#a?la=la-").getBytes)
    val bytes = messageDigest.digest
    val stringBuilder = new StringBuilder
    for (aByte <- bytes) {
      stringBuilder.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1))
    }
    stringBuilder.toString()
  }
}
