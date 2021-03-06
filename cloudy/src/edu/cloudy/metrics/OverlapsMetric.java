package edu.cloudy.metrics;

import edu.cloudy.geom.SWCRectangle;
import edu.cloudy.layout.LayoutResult;
import edu.cloudy.layout.WordGraph;
import edu.cloudy.nlp.Word;

import java.util.List;

/**
 * @author spupyrev
 * May 3, 2013
 */
public class OverlapsMetric implements QualityMetric
{
    //rectangles that are closer than EPS are considered as touching 
    private static double EPS = 0.005;

    @Override
    public double getValue(WordGraph wordGraph, LayoutResult algo)
    {
        List<Word> words = wordGraph.getWords();
        SWCRectangle bb = SpaceMetric.computeBoundingBox(words, algo);

        for (int i = 0; i < words.size(); i++)
            for (int j = i + 1; j < words.size(); j++)
            {
                if (overlap(algo, bb, words.get(i), words.get(j)))
                    return 1;
            }

        return 0;
    }

    private boolean overlap(LayoutResult algo, SWCRectangle bb, Word first, Word second)
    {
        SWCRectangle rect1 = algo.getWordPosition(first);
        SWCRectangle rect2 = algo.getWordPosition(second);
        return overlap(bb, rect1, rect2);
    }

    public static boolean overlap(SWCRectangle bb, SWCRectangle rect1, SWCRectangle rect2)
    {
        //checking interections manually, since we want to use EPS
        if (rect1 == null || rect2 == null)
            return false;
        boolean xIntersect = intersect(bb.getWidth(), rect1.getMinX(), rect1.getMaxX(), rect2.getMinX(), rect2.getMaxX());
        boolean yIntersect = intersect(bb.getHeight(), rect1.getMinY(), rect1.getMaxY(), rect2.getMinY(), rect2.getMaxY());
        return xIntersect && yIntersect;
    }

    private static boolean intersect(double size, double m1, double M1, double m2, double M2)
    {
        if (M1 - size * EPS <= m2)
            return false;
        if (M2 - size * EPS <= m1)
            return false;

        return true;
    }

}
