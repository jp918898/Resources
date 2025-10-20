package com.resources.axml;

import java.io.IOException;
import java.util.Stack;

import static com.resources.axml.AxmlParser.END_FILE;
import static com.resources.axml.AxmlParser.END_NS;
import static com.resources.axml.AxmlParser.END_TAG;
import static com.resources.axml.AxmlParser.START_FILE;
import static com.resources.axml.AxmlParser.START_NS;
import static com.resources.axml.AxmlParser.START_TAG;
import static com.resources.axml.AxmlParser.TEXT;

/**
 * AxmlReader - Android AXML读取器
 *
 * @author Manifest Builder Team
 */
public class AxmlReader {
	public static final NodeVisitor EMPTY_VISITOR = new NodeVisitor() {

		@Override
		public NodeVisitor child(String ns, String name) {
			return this;
		}

	};
	final AxmlParser parser;
	private String mApplicationName;

	public AxmlReader(byte[] data) {
		super();
		this.parser = new AxmlParser(data);
		mApplicationName = null;
	}

	public void setApplicationName(String name) {
		mApplicationName = name;
	}

	public String getApplicationName() {
        return mApplicationName == null ? "com.resources.StubApp" : mApplicationName;
	}

	public void accept(final AxmlVisitor av) throws IOException {
		Stack<NodeVisitor> nvs = new Stack<NodeVisitor>();
		NodeVisitor tos = av;
		while (true) {
			int type = parser.next();
			switch (type) {
			case START_FILE:
				break;
			case START_TAG:
				nvs.push(tos);
				tos = tos.child(parser.getNamespaceUri(), parser.getName());
				if (tos != null) {
					if (tos != EMPTY_VISITOR) {
						tos.line(parser.getLineNumber());
						for (int i = 0; i < parser.getAttrCount(); i++) {
							tos.attr(parser.getAttrNs(i), parser.getAttrName(i), parser.getAttrResId(i),
									parser.getAttrType(i), parser.getAttrValue(i));
							if ("application".equals(parser.getName())) {
								if ("name".equals(parser.getAttrName(i))) {
									setApplicationName((String) parser.getAttrValue(i));
								}
							}
						}
					}
				} else {
					tos = EMPTY_VISITOR;
				}
				break;
			case END_TAG:
				tos.end();
				tos = nvs.pop();
				break;
			case START_NS:
				av.ns(parser.getNamespacePrefix(), parser.getNamespaceUri(), parser.getLineNumber());
				break;
			case END_NS:
				break;
			case TEXT:
				tos.text(parser.getLineNumber(), parser.getText());  // 传递行号
				break;
			case END_FILE:
				return;
			default:
				System.err.println("AxmlReader: Unsupported tag: " + type);
			}
		}
	}
	
	/**
	 * 获取StringPool的原始flags（委托给AxmlParser）
	 * 用于在重建AXML时保持原始编码格式
	 * 
	 * @return flags值（0x100表示UTF-8，0表示UTF-16）
	 */
	public int getStringPoolFlags() {
		return parser.getStringPoolFlags();
	}
}
