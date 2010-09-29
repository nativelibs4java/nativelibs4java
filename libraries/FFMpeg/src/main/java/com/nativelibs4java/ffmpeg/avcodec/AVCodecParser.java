package com.nativelibs4java.ffmpeg.avcodec;
import org.bridj.Callback;
import org.bridj.Pointer;
import org.bridj.StructObject;
import org.bridj.ann.Array;
import org.bridj.ann.Field;
import org.bridj.ann.Library;
/**
 * <i>native declaration : libavcodec/avcodec.h</i><br>
 * This file was autogenerated by <a href="http://jnaerator.googlecode.com/">JNAerator</a>,<br>
 * a tool written by <a href="http://ochafik.free.fr/">Olivier Chafik</a> that <a href="http://code.google.com/p/jnaerator/wiki/CreditsAndLicense">uses a few opensource projects.</a>.<br>
 * For help, please visit <a href="http://nativelibs4java.googlecode.com/">NativeLibs4Java</a> or <a href="http://bridj.googlecode.com/">BridJ</a> .
 */
@Library("avcodec") 
public class AVCodecParser extends StructObject {
	public AVCodecParser() {
		super();
	}
	public AVCodecParser(Pointer pointer) {
		super(pointer);
	}
	/**
	 * several codec IDs are permitted<br>
	 * C type : int[5]
	 */
	@Array({5}) 
	@Field(0) 
	public Pointer<java.lang.Integer > codec_ids() {
		return this.io.getPointerField(this, 0);
	}
	@Field(1) 
	public int priv_data_size() {
		return this.io.getIntField(this, 1);
	}
	@Field(1) 
	public AVCodecParser priv_data_size(int priv_data_size) {
		this.io.setIntField(this, 1, priv_data_size);
		return this;
	}
	public final int priv_data_size_$eq(int priv_data_size) {
		priv_data_size(priv_data_size);
		return priv_data_size;
	}
	/// C type : parser_init_callback
	@Field(2) 
	public Pointer<AVCodecParser.parser_init_callback > parser_init() {
		return this.io.getPointerField(this, 2);
	}
	/// C type : parser_init_callback
	@Field(2) 
	public AVCodecParser parser_init(Pointer<AVCodecParser.parser_init_callback > parser_init) {
		this.io.setPointerField(this, 2, parser_init);
		return this;
	}
	/// C type : parser_init_callback
	public final Pointer<AVCodecParser.parser_init_callback > parser_init_$eq(Pointer<AVCodecParser.parser_init_callback > parser_init) {
		parser_init(parser_init);
		return parser_init;
	}
	/// C type : parser_parse_callback
	@Field(3) 
	public Pointer<AVCodecParser.parser_parse_callback > parser_parse() {
		return this.io.getPointerField(this, 3);
	}
	/// C type : parser_parse_callback
	@Field(3) 
	public AVCodecParser parser_parse(Pointer<AVCodecParser.parser_parse_callback > parser_parse) {
		this.io.setPointerField(this, 3, parser_parse);
		return this;
	}
	/// C type : parser_parse_callback
	public final Pointer<AVCodecParser.parser_parse_callback > parser_parse_$eq(Pointer<AVCodecParser.parser_parse_callback > parser_parse) {
		parser_parse(parser_parse);
		return parser_parse;
	}
	/// C type : parser_close_callback
	@Field(4) 
	public Pointer<AVCodecParser.parser_close_callback > parser_close() {
		return this.io.getPointerField(this, 4);
	}
	/// C type : parser_close_callback
	@Field(4) 
	public AVCodecParser parser_close(Pointer<AVCodecParser.parser_close_callback > parser_close) {
		this.io.setPointerField(this, 4, parser_close);
		return this;
	}
	/// C type : parser_close_callback
	public final Pointer<AVCodecParser.parser_close_callback > parser_close_$eq(Pointer<AVCodecParser.parser_close_callback > parser_close) {
		parser_close(parser_close);
		return parser_close;
	}
	/// C type : split_callback
	@Field(5) 
	public Pointer<AVCodecParser.split_callback > split() {
		return this.io.getPointerField(this, 5);
	}
	/// C type : split_callback
	@Field(5) 
	public AVCodecParser split(Pointer<AVCodecParser.split_callback > split) {
		this.io.setPointerField(this, 5, split);
		return this;
	}
	/// C type : split_callback
	public final Pointer<AVCodecParser.split_callback > split_$eq(Pointer<AVCodecParser.split_callback > split) {
		split(split);
		return split;
	}
	/// C type : AVCodecParser*
	@Field(6) 
	public Pointer<AVCodecParser > next() {
		return this.io.getPointerField(this, 6);
	}
	/// C type : AVCodecParser*
	@Field(6) 
	public AVCodecParser next(Pointer<AVCodecParser > next) {
		this.io.setPointerField(this, 6, next);
		return this;
	}
	/// C type : AVCodecParser*
	public final Pointer<AVCodecParser > next_$eq(Pointer<AVCodecParser > next) {
		next(next);
		return next;
	}
	/// <i>native declaration : libavcodec/avcodec.h:3734</i>
	public static abstract class parser_init_callback extends Callback<parser_init_callback > {
		public abstract int apply(Pointer<com.nativelibs4java.ffmpeg.avcodec.AVCodecParserContext > s);
	};
	/// <i>native declaration : libavcodec/avcodec.h:3735</i>
	public static abstract class parser_parse_callback extends Callback<parser_parse_callback > {
		public abstract int apply(Pointer<com.nativelibs4java.ffmpeg.avcodec.AVCodecParserContext > s, Pointer<com.nativelibs4java.ffmpeg.avcodec.AVCodecContext > avctx, Pointer<Pointer<java.lang.Byte > > poutbuf, Pointer<java.lang.Integer > poutbuf_size, Pointer<java.lang.Byte > buf, int buf_size);
	};
	/// <i>native declaration : libavcodec/avcodec.h:3739</i>
	public static abstract class parser_close_callback extends Callback<parser_close_callback > {
		public abstract void apply(Pointer<com.nativelibs4java.ffmpeg.avcodec.AVCodecParserContext > s);
	};
	/// <i>native declaration : libavcodec/avcodec.h:3740</i>
	public static abstract class split_callback extends Callback<split_callback > {
		public abstract int apply(Pointer<com.nativelibs4java.ffmpeg.avcodec.AVCodecContext > avctx, Pointer<java.lang.Byte > buf, int buf_size);
	};
}
