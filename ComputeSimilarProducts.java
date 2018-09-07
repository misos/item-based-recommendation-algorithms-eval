import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;


public class ComputeSimilarProducts {
    private final Map<String,double[]> productFeatures;
    private final Map<String,Double> productVectorLenghts;
    private final int features;
    private final int numProducts;

    
    public static ComputeSimilarProducts createComputeSimilarProductsFromFileTSV(String productFeaturesTSV, int features, int numProducts) throws UnsupportedEncodingException, IOException {
        Map<String,double[]> productFeatures = LocalModelRecommender.parseFeatures(productFeaturesTSV,features, -1);
        ComputeSimilarProducts csp = new ComputeSimilarProducts(productFeatures, features, numProducts);
        return csp;
    }

    public ComputeSimilarProducts(Map<String,double[]> productFeatures, int features, int numProducts) {
        this.productFeatures = productFeatures;
        this.features = features;
        this.numProducts = numProducts;
        this.productVectorLenghts = computeVectorLengths(productFeatures);
    }
    
    private Map<String, Double> computeVectorLengths(
            Map<String, double[]> featureVectors) {
        Map<String,Double> featureVectorLenghts = new HashMap<String,Double>();
        for (String vectorKey : featureVectors.keySet()) {
            double sum = 0;
            double[] vector = featureVectors.get(vectorKey);
            for (int i = 0; i < vector.length; i++) {
                sum += vector[i]*vector[i];
            }
            featureVectorLenghts.put(vectorKey, Math.sqrt(sum));
        }
        return featureVectorLenghts;
    }

    public List<ScoredItem> similarProducts(String product, double sim_threshold) {
        double[] productVector = productFeatures.get(product);
        double productLength = productVectorLenghts.get(product);
        
        List<ScoredItem> scoredProducts = new ArrayList<ScoredItem>();
        for (String p : productFeatures.keySet()) {
            if (product.equals(p)) 
                continue; //we do not want to recommend itself
            double score = dotProduct(productVector, productFeatures.get(p));//(productVectorLenghts.get(p)*productLength);
            if (score >= sim_threshold) {
                scoredProducts.add(new ScoredItem(p, score));
            }
        }
        Collections.sort(scoredProducts);
        if (scoredProducts.size() <= numProducts) {
            return scoredProducts;
        } else {
            return scoredProducts.subList(0, numProducts);
        }
    }
    
    
    private double dotProduct(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < features; i++) {
            sum += a[i] * b[i];    
        }
        return sum;
    }    

    public static void main(String[] args) throws UnsupportedEncodingException, IOException {
        String prefix = "/srv/tmp/RecSys/";
        String product_features = prefix + "orders_days-3409-3499_client-10458712099_text_product_features.txt";
        String product_similarity_out = prefix + "product_similarity_based_on_u2u_CF.txt";
        int features = 32;
        double sim_threshold = 0.1;
        int max_num_products = 200;
                
        System.out.println("ComputeSimilarProducts [features] [product_features] [product_similarity_out] [sim_threshold] [max_num_products]");

        if (args.length > 0) {
            features = Integer.valueOf(args[0]);
        }
        if (args.length > 1) {
            product_features = args[1];
        }
        if (args.length > 2) {
            product_similarity_out = args[2];
        }
        
        if (args.length > 3) {
            sim_threshold = Double.valueOf(args[3]);
        }
        if (args.length > 4) {
            max_num_products = Integer.valueOf(args[4]);
        }
        System.out.println("Running: ComputeSimilarProducts [" + 
                features+ "] [" + product_features + "] [" + 
                product_similarity_out + "] [" + 
                sim_threshold + "] [" + 
                max_num_products + "]"
                );

        ComputeSimilarProducts csp = createComputeSimilarProductsFromFileTSV(product_features, features, max_num_products);
        csp.computeSimililarityProducyts(product_similarity_out, sim_threshold);
        

    }

    private void computeSimililarityProducyts(String product_similarity_out, double sim_threshold) throws FileNotFoundException {
        PrintWriter pw = new PrintWriter(product_similarity_out);
        for (String p : productFeatures.keySet()) {
            List<ScoredItem> scoredItems = similarProducts(p, sim_threshold);
            if (scoredItems != null && !scoredItems.isEmpty()) {
                pw.println(p + "\t" + StringUtils.join(scoredItems, " "));
            }
        }
        pw.close();        
        
    }

}
