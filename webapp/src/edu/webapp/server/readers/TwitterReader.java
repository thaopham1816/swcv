package edu.webapp.server.readers;

import edu.cloudy.nlp.ContextDelimiter;
import twitter4j.Query;
import twitter4j.Query.ResultType;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads tweets by a given query
 * Example:
 *   twitter: query size:500 type:[recent,popular,mixed] lang:en include:retweets
 */
public class TwitterReader implements IDocumentReader, ISentimentReader
{
    private static final String UNWANTED_PATTERN = "\\s*http[s]?://\\S+\\s*";
    private static final int DEFAULT_NUMBER_OF_TWEETS = 300;

    private String tweetsText;

    public boolean isConnected(String input)
    {
        if (!input.startsWith("twitter:"))
            return false;

        SearchQuery sq = new SearchQuery(input.substring(8).trim());
        sq.parse();
        if (!sq.isValidQuery())
            return false;

        try
        {
            List<String> tweetsList = searchTweets(sq);
            tweetsText = concatTweets(tweetsList);
            return true;
        }
        catch (TwitterException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public String getText(String input)
    {
        return tweetsText;
    }

    private List<String> searchTweets(SearchQuery sq) throws TwitterException
    {
        List<String> resTweets = new ArrayList();

        Twitter twitter = TwitterReader.getTwitterInstance();
        Query query = new Query(sq.getSearchPhrase());
        query.setCount(sq.getSize());
        query.setResultType(sq.getResultType());
        if (sq.getLang() != null)
            query.setLang(sq.getLang());

        int downloadCount = 0;
        long lowestTweetId = Long.MAX_VALUE;
        QueryResult result;
        while (downloadCount < sq.getSize())
        {
            query.setMaxId(lowestTweetId - 1);
            result = twitter.search(query);
            List<Status> tweets = result.getTweets();
            for (Status tweet : tweets)
            {
                if (tweet.isRetweet() && !sq.isIncludeRetweets())
                    continue;

                lowestTweetId = Math.min(lowestTweetId, tweet.getId());
                resTweets.add(removeLinks(tweet.getText()) + "." + ContextDelimiter.SENTIMENT_DELIMITER_TEXT + "\n");
                downloadCount++;
            }

            if (result.hasNext())
                query = result.nextQuery();
            else
                break;
        }

        return resTweets;
    }

    private String concatTweets(List<String> tweets)
    {
        StringBuffer sb = new StringBuffer();
        tweets.forEach(s -> sb.append(s));
        return sb.toString();
    }

    private String removeLinks(String tweet)
    {
        return tweet.replaceAll(UNWANTED_PATTERN, "");
    }

    public static Twitter getTwitterInstance()
    {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(false);
        cb.setOAuthConsumerKey(TwitterCredentials.CONSUMER_KEY);
        cb.setOAuthConsumerSecret(TwitterCredentials.CONSUMER_SECRET);
        cb.setOAuthAccessToken(TwitterCredentials.ACCESS_TOKEN);
        cb.setOAuthAccessTokenSecret(TwitterCredentials.ACCESS_TOKEN_SECRET);
        TwitterFactory tf = new TwitterFactory(cb.build());
        return tf.getInstance();
    }

    static class SearchQuery
    {
        private String input;
        private String searchPhrase;

        private int size = DEFAULT_NUMBER_OF_TWEETS;
        private ResultType resultType = Query.RECENT;
        private String lang;
        private boolean includeRetweets = false;

        public SearchQuery(String input)
        {
            this.input = input;
        }

        public void parse()
        {
            //ex: uofa size:500 type:[recent,popular,mixed] lang:en include:retweets
            String sz = extractOption("size:(\\d+)");
            if (sz != null)
            {
                size = Integer.valueOf(sz);
                size = Math.min(size, 5000);
            }

            String st = extractOption("type:(\\w+)");
            if ("recent".equalsIgnoreCase(st))
                resultType = Query.RECENT;
            else if ("popular".equalsIgnoreCase(st))
                resultType = Query.POPULAR;
            else if ("mixed".equalsIgnoreCase(st))
                resultType = Query.MIXED;

            String se = extractOption("lang:(\\w+)");
            lang = se;

            String sr = extractOption("include:(\\w+)");
            if ("retweets".equalsIgnoreCase(sr))
                includeRetweets = true;

            searchPhrase = input.trim();
        }

        private String extractOption(String pattern)
        {
            Pattern typePattern = Pattern.compile(pattern);
            Matcher typeMatcher = typePattern.matcher(input);
            if (typeMatcher.find())
            {
                String res = typeMatcher.group(1);
                input = typeMatcher.replaceAll("");
                return res;
            }

            return null;
        }

        public int getSize()
        {
            return size;
        }

        public String getSearchPhrase()
        {
            return searchPhrase;
        }

        public ResultType getResultType()
        {
            return resultType;
        }

        public String getLang()
        {
            return lang;
        }

        public boolean isIncludeRetweets()
        {
            return includeRetweets;
        }

        public boolean isValidQuery()
        {
            return searchPhrase.length() > 0;
        }
    }
}
