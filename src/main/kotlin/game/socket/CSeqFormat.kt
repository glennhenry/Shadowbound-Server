package game.socket

import encore.network.fanchant.Fanchant
import encore.network.fanchant.guide.DecodeResult
import encore.network.fanchant.guide.FanchantGuide

class CSeqFormat : FanchantGuide<String> {
    override fun verify(data: ByteArray): Boolean {
        return true
    }

    override fun tryDecode(data: ByteArray): DecodeResult<String> {
        TODO()
    }

    override fun materialize(decoded: String): Fanchant {
        TODO()
    }
}

object CSeqSerializer {
    // TODO()
}

/**
 * Represent an element in CSeq format.
 *
 * @property eleType Type constants of the element.
 * @property eleName Element name.
 * @property eleObject The element object's class if this element is an object type.
 */
data class CSeqEleInfo(
    val eleType: String = CSeqEleType.TYPE_UNKNOW,
    val eleName: String = "",
    val eleObject: String = ""
) {
    init {
        require(eleType in CSeqEleType.ALL) { "Wrong eleType constant: $eleType" }
    }
}

object CSeqEleType {
    const val TYPE_UNKNOW: String = "unknow"
    const val TYPE_INT8: String = "int8"
    const val TYPE_UINT8: String = "uint8"
    const val TYPE_INT16: String = "int16"
    const val TYPE_UINT16: String = "uint16"
    const val TYPE_INT32: String = "int32"
    const val TYPE_UINT32: String = "uint32"
    const val TYPE_FLOAT: String = "float"
    const val TYPE_DOUBLE: String = "double"
    const val TYPE_STRING: String = "string"
    const val TYPE_OBJECT: String = "object"
    const val TYPE_ARRAY: String = "array"
    const val TYPE_MAP: String = "map"
    const val TYPE_INDICES: String = "indices"
    const val TYPE_BYTES: String = "bytes"
    val ALL = setOf(
        TYPE_UNKNOW,
        TYPE_INT8,
        TYPE_UINT8,
        TYPE_INT16,
        TYPE_UINT16,
        TYPE_INT32,
        TYPE_UINT32,
        TYPE_FLOAT,
        TYPE_DOUBLE,
        TYPE_STRING,
        TYPE_OBJECT,
        TYPE_ARRAY,
        TYPE_MAP,
        TYPE_INDICES,
        TYPE_BYTES
    )
}

/*
zlib.decompress, decode utf8:
- badword.cbm
- name.cbm
- player.cbm

other:
- effectdata.cbm
- playerdata.cbm
- sounddata.cbm
 */

/*
About CSeq
==========

uses a format called CSeq
consist of "head" (SPackHead) and "body" (SMsgHead, consisting a list of CSeqEleInfo)
_play/Network.as is networking code to send/write message in socket
in the client side, must supply SMsgHead in send() call

CSeq
| a field in CSeq format is a CSeqEleInfo
extended by SMsgHead (base message)

e.g., SSystemQ represent a message containing system information
	  SSystemQProxy represent a proxy detail (server host and port)

each implementation overrides message type/option as needed, and extend the field in the base message

CSeq
- CSeqEleInfo
	TYPE_UNKNOW: String = "unknow"
	TYPE_INT8: String = "int8"
	TYPE_UINT8: String = "uint8"
	TYPE_INT16: String = "int16"
	TYPE_UINT16: String = "uint16"
	TYPE_INT32: String = "int32"
	TYPE_UINT32: String = "uint32"
	TYPE_FLOAT: String = "float"
	TYPE_DOUBLE: String = "double"
	TYPE_STRING: String = "string"
	TYPE_OBJECT: String = "object"
	TYPE_ARRAY: String = "array"
	TYPE_MAP: String = "map"
	TYPE_INDICES: String = "indices"
	TYPE_BYTES: String = "bytes"
	- eleType: String
	- eleName: String
	- eleObject: *
SMsgHead
- msg_type     : uint
- msg_type     : uint
- msg_option   : uint
- seqno        : uint
- head_nouse   : uint
- session_id   : uint
- role_id      : uint
- outside_sock : int
- static_element_info : List<CSeqEleInfo> =
	- CSeqEleInfo(uint16, "msg_type")
	- CSeqEleInfo(uint16, "msg_option")
	- CSeqEleInfo(uint16, "seqno")
	- CSeqEleInfo(uint16, "head_nouse")
	- CSeqEleInfo(uint32, "session_id")
	- CSeqEleInfo(uint32, "role_id")
	- CSeqEleInfo(int32, "outside_sock")
SSystemQ override
	msg_type = 1
	elementInfo adds empty list
SSystemQProxy override
	msg_option = 31
	elementInfo adds
		- CSeqEleInfo(string, "host")
		- CSeqEleInfo(uint16, "port")

public function CSeqEleInfo(param1:String, param2:String, param3:* = null) {
    super();
    this.eleType = param1;
    this.eleName = param2;
    this.eleObject = param3;
}

list of constants proxy/struct/protocol.as
some message may be deferred (locked) because
- network is intentionally locked
- message is "not important"
	- msg_type is not equal to protocol.TYPE_SYSTEMQ
	- msg_option is not equal to either OPTION_SYSTEM_LOGIN, OPTION_SYSTEM_RECONNECT, OPTION_SYSTEM_PROXY, OPTION_SYSTEM_PING, OPTION_SYSTEM_TEST
	- in other word, only login/reconnect/ping/etc message will always be send to server and never locked

then message packaging structure:
- packaged with SPackHead

SPackHead
- pack_flag     : uint = 57799
- pack_st       : uint = 0
- pack_crypt    : uint = 1
- pack_length   : uint = 0
- pack_checksum : uint = 0
- static_element_info : List<CSeqEleInfo> =
	- CSeqEleInfo(uint16, "pack_flag")
	- CSeqEleInfo(uint8, "pack_st")
	- CSeqEleInfo(uint8, "pack_crypt")
	- CSeqEleInfo(uint32, "pack_length")
	- CSeqEleInfo(uint32, "pack_checksum")

 */

/*
CSeq encoding
=============

encoding:
    given a SMsgHead
    create mainbuffer
    call SMsgHead.write(mainbuffer)
    switch mainbuffer to little endian
    for each elementInfo
        call writeStream(mainbuffer:bytearray, value:any, cseleqinfo)

    writeStream():
    int8, uint8 -> writeByte
    int16, uint16 -> writeShort
    int32 -> writeInt
    uint32 -> writeUnsignedInt
    float -> writeFloat
    double -> writeDouble
    string -> create temp buffer, little endian, writeMultiByte of value (param2), "utf-8",
              to main buffer, writeShort of the buffer length
              to main buffer, writeBytes of the temp buffer
    object -> to param2 write param1
    array -> writeShort of array length
             for each el in array, writeStream(mainbuffer, el, cseleqinfo.eleobject)
    map -> create temp buffer, little endian
           to main buffer, writeShort of map length
           for each key in the map
             to temp buffer, writeMultiByte of the value as "utf-8"
             change buffer pointer position to 0
             to main buffer, writeShort of the temp buffer length
             to main buffer, write the temp buffer
             call writeStream(mainbuffer, map[key], cseleqinfo.eleobject)
    indices (for map with number as key) -> writeShort of the map (param2) length
                                            for each key (string typed but is uint) in the map
                                              to main buffer, writeUnsignedInt of the key
                                              call writeStream(mainbuffer, map[key], cseleqinfo.eleobject
    bytes -> writeUnsignedInt of the value length
             writeBytes of the bytes value

if msg_type == TYPE_SYSTEMQ and  msg_option is any of OPTION_SYSTEM_LOGIN
                                                      OPTION_SYSTEM_RECONNECT
                                                      OPTION_SYSTEM_PROXY
                                                      OPTION_SYSTEM_PING
                                                      OPTION_SYSTEM_TEST
    set pack_crypt = 0
    set pack_length to mainbuffer length
    set pack_checksum to checksum of mainbuffer

    getChecksum(bytearray) - CSeq:
    summary: checksum is the sum of all bytes in the bytearray
    details:
        if bytearray is null, write empty bytearray and set empty flag to true
        get sum of all bytes in the bytearray
        if empty flag is true, clear the given bytearray
        return sum of all bytes in the bytearray

    if vSendLog is enabled
        to vSendLog buffer, writeUnsignedInt of current time - last send time
        to vSendLog buffer, write bytes of the mainbuffer

    if pack_crypt != 0 and GameCoder.isEncryptInit == true (default = false)
        encrypt the data (just disable if possible)
    else
        set pack_crypt = 0

    create newbuffer
    call SPackHead.write(newbuffer)
    write mainbuffer to the newbuffer
    send the newbuffer
 */
