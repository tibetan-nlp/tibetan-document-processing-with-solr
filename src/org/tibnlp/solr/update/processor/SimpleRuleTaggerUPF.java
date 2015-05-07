package org.tibnlp.solr.update.processor;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.nio.charset.Charset;

import org.apache.solr.update.processor.FieldMutatingUpdateProcessor;
import org.apache.solr.update.processor.FieldValueMutatingUpdateProcessor;
import org.apache.solr.update.processor.FieldMutatingUpdateProcessorFactory;
import org.apache.solr.update.processor.UpdateRequestProcessor;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleRuleTaggerUPF extends FieldMutatingUpdateProcessorFactory {
    private final static Logger log = LoggerFactory.getLogger(SimpleRuleTaggerUPF.class);
    
    private static final String RULES_PARAM = "rules";
    private List<Pattern> patterns = new LinkedList<Pattern>();
    private List<String> replaces = new LinkedList<String>();

	@SuppressWarnings("unchecked")
	@Override
	public void init(NamedList args) {
	    Object rulesParam = args.remove(RULES_PARAM);
	    if (null == rulesParam || !(rulesParam instanceof String)) {
	        //
	    }
	    else {
	        try {
                String ruleFile = (String)rulesParam;
                InputStream in = new FileInputStream(ruleFile);
                BufferedReader br = new BufferedReader(new InputStreamReader(in, Charset.forName("UTF-8")));
                String line;
                while ((line = br.readLine()) != null) {
                    if (!(line.startsWith("#") || line.length()==0)) { //ignore
                        String[] rule = line.split(">");
                        try {
                            log.info("Compiling: " + line);
                            patterns.add(Pattern.compile(rule[0].trim()));
                            replaces.add(rule[1].trim());
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            //error
                        }
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
	    }
        super.init(args);
    }
    
    @Override
    public FieldMutatingUpdateProcessor.FieldNameSelector getDefaultSelector(final SolrCore core) {
        return FieldMutatingUpdateProcessor.SELECT_NO_FIELDS;
    }
  
    @Override
    public UpdateRequestProcessor getInstance(SolrQueryRequest req, SolrQueryResponse rsp, UpdateRequestProcessor next) {
        return new FieldValueMutatingUpdateProcessor(getSelector(), next) {
            protected Object mutateValue(final Object src) {
                if (src instanceof CharSequence) {
                    CharSequence s = (CharSequence)src;
                    String tString = s.toString();
                    
                    Iterator<Pattern> iter = patterns.iterator();
                    Iterator<String> iter2 = replaces.iterator();
                    while (iter.hasNext()) {
                        Pattern pattern = iter.next();
                        String replace = iter2.next();
                        tString = pattern.matcher(tString).replaceAll(replace);
                    }
                    
                    return tString;
                }
                else 
                {
                    return src;
                }
            }
        };
    }
}
