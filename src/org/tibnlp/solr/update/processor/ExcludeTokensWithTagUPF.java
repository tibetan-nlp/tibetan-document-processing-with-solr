package org.tibnlp.solr.update.processor;

import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.regex.Pattern;

import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.BufferedReader;

import org.apache.commons.lang3.StringUtils;

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

public class ExcludeTokensWithTagUPF extends FieldMutatingUpdateProcessorFactory {
    private final static Logger log = LoggerFactory.getLogger(PruneMinorUPF.class);

    private static final String TAG_DELIMITER_PARAM = "tagDelimiter";
    private static final String TAG_DELIMITER_DEFAULT = "|";
    private static final String DELIMIT_OUTPUT_PARAM = "delimitOutput";
    private static final String DELIMIT_OUTPUT_DEFAULT = " ";
    private static final String EXCLUDE_TAGS_PARAM = "excludeTags";
    private String tagDelimiter, delimitOutput;

    private Set<String> exclusions = new HashSet<String>();

    @SuppressWarnings("unchecked")
    @Override
    public void init(NamedList args) {
        Object tagDelimiterParam = args.remove(TAG_DELIMITER_PARAM);
        if (null == tagDelimiterParam || !(tagDelimiterParam instanceof String)) {
            tagDelimiter = TAG_DELIMITER_DEFAULT;
        }
        else {
            tagDelimiter = (String)tagDelimiterParam;
        }

        Object delimitOutputParam = args.remove(DELIMIT_OUTPUT_PARAM);
        if (null == delimitOutputParam || !(delimitOutputParam instanceof String)) {
            delimitOutput = DELIMIT_OUTPUT_DEFAULT;
        }
        else {
            delimitOutput = (String)delimitOutputParam;
        }

        Object excludeTagsParam = args.remove(EXCLUDE_TAGS_PARAM);
        if (null == excludeTagsParam || !(excludeTagsParam instanceof String)) {
            //error
        }
        else {
            exclusions = new HashSet<String>(Arrays.asList(((String)excludeTagsParam).split("\\s+")));
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
                    String[] tokens = s.toString().split("\\s+");
                    List<String> words = new LinkedList<String>();
                    Pattern pattern = Pattern.compile("[\\[\\]]+");

                    for (int j=0; j<tokens.length; j++) {
                        int n = tokens[j].indexOf(tagDelimiter);

                        String tagString = tokens[j].substring(n+1);
                        if (tagString.charAt(0) != '[') {
                            if (!exclusions.contains(tagString)) {
                                words.add(tokens[j]);
                            }
                        }
                        else {
                            String[] tags = pattern.split(tagString.substring(1));
                            Set<String> major = new TreeSet<String>();
                            boolean exclude = false;
                            for (int k=0; k<tags.length; k++) {
                                if (exclusions.contains(tags[k])) {
                                    exclude = true;
                                    break;
                                }
                            }
                            if (!exclude) {
                                words.add(tokens[j]);
                            }
                        }
                    }
                    return StringUtils.join(words.iterator(), delimitOutput);
                }
                else
                {
                    return src;
                }
            }
        };
    }
}