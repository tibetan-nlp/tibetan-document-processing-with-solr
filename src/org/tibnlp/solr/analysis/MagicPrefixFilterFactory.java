package org.tibnlp.solr.analysis;

import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

public class MagicPrefixFilterFactory extends TokenFilterFactory {
    
    public MagicPrefixFilterFactory(Map<String,String> args) {
        super(args);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }
  
    @Override
    public MagicPrefixTokenFilter create(TokenStream input) {
        return new MagicPrefixTokenFilter(input);
    }
}
