package com.example.camelsalesforce.processors;

import java.util.HashMap;
import java.util.Map;

public class FieldMapping {

	public static final FieldMapping TRANSLATE = createMapping();

	private Map<String, FieldMappingEntry> mappings = new HashMap<>();

	private static FieldMapping createMapping() {
		FieldMapping fieldMapping = new FieldMapping();
		fieldMapping.addMapping("policyID", "Policy_ID__c", String.class);
		fieldMapping.addMapping("statecode", "State_Code__c", String.class);
		return fieldMapping;
	}
	
	public FieldMappingEntry get(String srcName) {
		return mappings.get(srcName.toLowerCase());
	}
	
	private void addMapping(String srcName, String dstName, Class dataType) {
		mappings.put(srcName.toLowerCase(), new FieldMappingEntry(srcName, dstName, dataType));
	}
	
	public static class FieldMappingEntry {
		private String srcName;
		private String dstName;
		private Class dataType;

		private FieldMappingEntry(String srcName, String dstName, Class dataType) {
			this.srcName = srcName;
			this.dstName = dstName;
			this.dataType = dataType;
		}
		
		public String getSrcName() {
			return srcName;
		}

		public String getDstName() {
			return dstName;
		}

		public Class getDataType() {
			return dataType;
		}

		@Override
		public String toString() {
			return "FieldMappingEntry [srcName=" + srcName + ", dstName=" + dstName + ", dataType=" + dataType + "]";
		}
	}
}
