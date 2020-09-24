package uk.nhs.nhsx.virology.order;

public class VirologyWebsiteConfig {
    
    public final String orderWebsite;
    public final String registerWebsite;

    public VirologyWebsiteConfig(String orderWebsite, String registerWebsite) {
        this.orderWebsite = orderWebsite;
        this.registerWebsite = registerWebsite;
    }
}