package models

import scodec.Codec
import scodec.codecs._
import shapeless.HNil
import shapeless.syntax.singleton._

sealed trait PQMessage

final case class ReqPQ(nonce: Long) extends PQMessage

final case class ResPQ(
  nonce:        Long,
  serverNonce:  Long,
  pq:           String,
  fingerprints: List[Long]
) extends PQMessage

final case class PqInnerData(
  pq:          String,
  p:           String,
  q:           String,
  nonce:       Long,
  serverNonce: Long,
  newNonce:    Long
)

final case class EncyptedPqInnerData(
  newNonce:      Long,
  data:          String,
  dataWithHash:  String,
  encryptedData: String
)

final case class ReqDHParams(
  nonce:                Long,
  serverNonce:          Long,
  p:                    String,
  q:                    String,
  publicKeyFingerprint: Long,
  encryptedData:        String
) extends PQMessage

final case class ResDHParamsOk(nonce: Long, serverNonce: Long) extends PQMessage

final case class ResDHParamsFail(nonce: Long, serverNonce: Long)
    extends PQMessage

object PQMessage {

  implicit def reqPQCodec: Codec[ReqPQ] =
    ("nonce" | int64).as[ReqPQ]
  implicit def resPqCodec: Codec[ResPQ] = {
    ("nonce" | int64) ::
    ("server_nonce" | int64) ::
    ("pq" | ascii32) ::
    ("fingerprints" | list(int64L))
  }.as[ResPQ]
  implicit def pqInnerDataCodec: Codec[PqInnerData] = {
    ("constructor number" | constant(int32L.encode(0x83c95aec).require)) ::
    ("pq" | ascii32) ::
    ("p" | ascii32) ::
    ("q" | ascii32) ::
    ("nonce" | int64) ::
    ("server_nonce" | int64) ::
    ("new_nonce" | int64)
  }.as[PqInnerData]
  implicit def encryptedPqInnerDataCodec: Codec[EncyptedPqInnerData] = {
    ("new_nonce" | int64) ::
    ("data" | ascii32) ::
    ("data_with_hash" | ascii32) ::
    ("encrypted_data" | ascii32)
  }.as[EncyptedPqInnerData]
  implicit def reqDHCodec: Codec[ReqDHParams] = {
    ("nonce" | int64) ::
    ("server_nonce" | int64) ::
    ("p" | ascii32) ::
    ("q" | ascii32) ::
    ("public_key_fingerprint" | int64) ::
    ("encrypted_data" | ascii32)
  }.as[ReqDHParams]
  implicit def resDHOkCodec: Codec[ResDHParamsOk] = {
    ("nonce" | int64) :: ("server_nonce" | int64)
  }.as[ResDHParamsOk]
  implicit def resDHFailCodec: Codec[ResDHParamsFail] = {
    ("nonce" | int64) :: ("server_nonce" | int64)
  }.as[ResDHParamsFail]
  val pqMsgCodec: Codec[PQMessage] = {
    Codec
      .coproduct[PQMessage]
      .discriminatedBy(int32)
      .using(
        Symbol("ReqPQ") ->> 0x60469778 ::
        Symbol("ResPQ") ->> 0x05162463 ::
        Symbol("ReqDHParams") ->> 0xd712e4be ::
        Symbol("ResDHParamsOk") ->> 0xd0e8075c ::
        Symbol("ResDHParamsFail") ->> 0x79cb045d ::
        HNil
      )
      .as[PQMessage]
  }
}

final case class UnencryptedMessage(
  authKeyId: Long,
  messageId: Long,
  message:   PQMessage
)

object UnencryptedMessage {
  val codec: Codec[UnencryptedMessage] = {
    ("auth_key_id" | int64L) :: ("message_Id" | int64L) :: variableSizeBytes(
      int32L,
      PQMessage.pqMsgCodec
    )
  }.as[UnencryptedMessage]

  def toBytes(msg: UnencryptedMessage): Array[Byte] =
    codec
      .encode(msg)
      .require
      .bytes
      .toArray
}
