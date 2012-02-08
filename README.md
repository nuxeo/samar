# Semantic analysis for the Arabic language

http://samar.fr/

This repo hosts the configuration and links or source of integration
components for the Samar project.

The integration (currently) requires the setup of:

* a Nuxeo server for hosting the source document repository and the result of
  the extracted semantic annoations

* a Temis Luxid Annotation Factory web serice to analyze the text of the
  documents.

* a Mondeca ITM web service to handle the taxonomies and geo location
  multi-lingual descriptions.

* a Stanbol server used as a middleware to integrate LAF and ITM with Nuxeo.

* an Antidot search server configured to index the Nuxeo documents using the
  CMIS interface and facets defined in the Mondeca ITM service

More to come...


## Stanbol launcher

## Nuxeo addons deployment

* nuxeo-platform-semanticentities: Stanbol connector

* nuxeo-newsml: document type configuration for handling NewsML document types


### Environment variables

The following variables are required by the Samar stanbol launcher to access
the Temis Luxid and Mondeca ITM web services.

    export STANBOL_TEMIS_SERVICE_WSDL_URL=http://examples.com/LuxidWS/services/Annotation?wsdl
    export STANBOL_TEMIS_SERVICE_ACCOUNT_ID=myuserid
    export STANBOL_TEMIS_SERVICE_ACCOUNT_PASSWORD=mypassword
    export STANBOL_TEMIS_SERVICE_ANNOTATION_PLAN=Entities
