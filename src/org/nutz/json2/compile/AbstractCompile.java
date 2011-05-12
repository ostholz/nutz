package org.nutz.json2.compile;

import java.io.IOException;
import java.io.Reader;

import org.nutz.json.JsonException;
import org.nutz.json2.JsonCompile;
import org.nutz.json2.JsonItem;
import org.nutz.json2.item.SingleJsonItem;
import org.nutz.json2.item.StringJsonItem;

public abstract class AbstractCompile implements JsonCompile{
	
	protected Reader reader;
	protected int cursor;
	protected int col;
	protected int row;
	
	public JsonItem Compile(Reader reader) {
		this.reader = reader;
		try {
			nextChar();
			skipCommentsAndBlank();
			if(cursor == 'v'){
				/*
				 * Meet the var ioc ={ maybe, try to find the '{' and break
				 */
				while (-1 != nextChar())
					if ('{' == cursor)
						break;
			}
			return compileLocation();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected abstract JsonItem compileLocation() throws IOException;
	
	protected int nextChar() throws IOException {
		if (-1 == cursor)
			return -1;
		try {
			cursor = reader.read();
			if (cursor == '\n') {
				row++;
				col = 0;
			} else
				col++;
		}
		catch (Exception e) {
			cursor = -1;
		}
		return cursor;
	}
	protected SingleJsonItem readString() throws IOException{
		if(cursor != '\'' && cursor != '"'){
			return new SingleJsonItem(readNoWarpString().toString());
		}
		return new StringJsonItem(readWarpString().toString());
	}
	protected StringBuilder readNoWarpString() throws IOException{
		StringBuilder sb = new StringBuilder();
		while(cursor != -1 && cursor != ':' && cursor != ',' && cursor != ']' && cursor != '}'){
			sb.append((char)cursor);
			nextChar();
			skipCommentsAndBlank();
		}
		return sb;
	}
	protected StringBuilder readWarpString() throws IOException {
		StringBuilder sb = new StringBuilder();
		int expEnd = cursor;
		nextChar();
		while (cursor != -1 && cursor != expEnd) {
			if (cursor == '\\') {
				nextChar();
				switch (cursor) {
				case 'n':
					cursor = 10;
					break;
				case 'r':
					cursor = 13;
					break;
				case 't':
					cursor = 9;
					break;
				case 'u':
					char[] hex = new char[4];
					for (int i = 0; i < 4; i++)
						hex[i] = (char) nextChar();
					cursor = Integer.valueOf(new String(hex), 16);
					break;
				case 'b':
					throw makeError("don't support \\b");
				case 'f':
					throw makeError("don't support \\f");
				}
			}
			sb.append((char) cursor);
			nextChar();
		}
		if (cursor == -1)
			throw makeError("Unclose string");
		nextChar();
		skipCommentsAndBlank();
		return sb;
	}
	
	protected void skipCommentsAndBlank() throws IOException {
		skipBlank();
		while (cursor == '/') {
			nextChar();
			if (cursor == '/') { // inline comment
				skipInlineComment();
				nextChar();
			} else if (cursor == '*') { // block comment
				skipBlockComment();
				nextChar();
			} else {
				throw makeError("Error comment syntax!");
			}
			skipBlank();
		}
	}
	protected void skipInlineComment() throws IOException {
		while (nextChar() != -1 && cursor != '\n') {}
	}
	
	protected void skipBlank() throws IOException {
		while (cursor >= 0 && cursor <= 32)
			nextChar();
	}

	protected void skipBlockComment() throws IOException {
		nextChar();
		while (cursor != -1) {
			if (cursor == '*') {
				if (nextChar() == '/')
					break;
			} else
				nextChar();
		}
	}
	protected JsonException makeError(String message) {
		return new JsonException(row, col, (char) cursor, message);
	}
}