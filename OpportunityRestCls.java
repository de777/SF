/*
Description: This class will be called from the *** component to send the opportunity details to 
Created Date: 30/10/2020


*/

public class OpportunityRestCls {
   
    @AuraEnabled public static ORG_Integration_Settings__c config {get;set;}
    @AuraEnabled public static Stage_Integration_Setting__c sist {get;set;}
    @AuraEnabled public static Cluster_Integration_Setting__c cist {get;set;}
    @AuraEnabled public static Stage_Access_Setting__c sast {get;set;}
    @AuraEnabled public static Quick_Action_Access_Setting__c qaas {get;set;}
    
    
    // Method to prepare JSON and then send opportunity data
    @AuraEnabled 
    public static void sendOppData(ID oppID)
    {   
        
        // get current user email address to check whether the user is authorized to send the opportunity data to ORG
        String userName = UserInfo.getUserName();
        User activeUser = [Select Email From User where Username = : userName limit 1];
        String userEmail = activeUser.Email;
        system.debug('Taha' + userEmail);
        try
        {
            qaas = Quick_Action_Access_Setting__c.getValues(userEmail);   
        }
        catch(Exception ex)
        {
            system.debug(ex.getMessage());
            throw new AuraHandledException('Error: You are not authorized to perform this action.');
        }
           
        // first query the required fields from the opportunity
        if(qaas != null)
        {
            if(oppID != null)
            {
                string jsonBody = '';
                Opportunity opp = new Opportunity();         

                opp =  [select ID, name, description, Investor__c, Investment_Model__c, Investor_Name__c,
                            Job_Creation__c, Location_1__c, Location_2__c, Location_3__c, Amount,Critical_Enabler__c,
                            Account.Name,Opportunity_specific_value_Proposition__c, Rationale_for_this_Opportunity__c,
                            Risks_and_Potential_Mitigation_Actions__c,StageName,Sector__c,Cluster__c, Timeline__c,Incentives_Package__c
                            from opportunity where ID=: oppID];
                 
                if(opp != null)
                {
                    try
                    {
                        sast = Stage_Access_Setting__c.getvalues(opp.StageName);
                    }
                    catch(Exception ex)
                    {
                        system.debug(ex.getMessage());
                        throw new AuraHandledException('Error: Opportunity Details not found.'); 
                    }
                    
					if(sast != null && sast.ORG_Value__c == true)
                    {
                        // get the data from opportunity record and then parse it
                        try
                        {
                            cist = Cluster_Integration_Setting__c.getValues(opp.Cluster__c);
                            sist = Stage_Integration_Setting__c.getValues(opp.StageName);
                            system.debug('sist value: ' + sist);
                            system.debug('cist value: ' + cist);
                            jsonBody = '{';
                            jsonBody += '"Gate__c":"' + (sist.ORG_Value__c != null && sist.ORG_Value__c != '' ? sist.ORG_Value__c : '') + '",';
                            jsonBody += '"Sector__c":"' + (cist.ORG_Value__c != null && cist.ORG_Value__c != '' ? cist.ORG_Value__c : '') + '",';
                            jsonBody += '"Description_of_opportunity__c":"' + (opp.description != null && opp.description != '' ? opp.description : '') + '",';
                            jsonBody += '"Identification_of_the_right_Partner__c":"' + (opp.Investor_Name__c != null && opp.Investor_Name__c != '' ? opp.Investor_Name__c : '') + '",';
                            jsonBody += '"Investment_Model__c":"' + (opp.Investment_Model__c != null && opp.Investment_Model__c != '' ? opp.Investment_Model__c : '') + '",';
                            jsonBody += '"Job_Creation__c":' +  (opp.Job_Creation__c != null ? opp.Job_Creation__c : null) + ',';
                            jsonBody += '"Opportunity_Name__c":"' + (opp.Name != null && opp.Name != '' ? opp.Name : '') + '",';
                            jsonBody += '"Name":"' + (opp.Name != null && opp.Name != '' ? opp.Name : '') + '",';
                            jsonBody += '"Opportunity_Size__c":' + (opp.Amount != null ? opp.Amount : null) + ',';
                            jsonBody += '"Opportunity_specific_Enablers__c":"' + (opp.Critical_Enabler__c != null && opp.Critical_Enabler__c != '' ?opp.Critical_Enabler__c : '') + '",';
                            jsonBody += '"Opportunity_specific_stakeholders__c":"' + (opp.Account.Name != null && opp.Account.Name != '' ? opp.Account.Name : '') + '",';
                            jsonBody += '"Opportunity_specific_value_Proposition__c":"' + (opp.Opportunity_specific_value_Proposition__c != null && opp.Opportunity_specific_value_Proposition__c != '' ? opp.Opportunity_specific_value_Proposition__c : '')  + '",';
                            jsonBody += '"Rationale_for_this_Opportunity__c":"' + (opp.Rationale_for_this_Opportunity__c != null && opp.Rationale_for_this_Opportunity__c != '' ? opp.Rationale_for_this_Opportunity__c : '') + '",';
                            jsonBody += '"Risks_and_Potential_Mitigation_Actions__c":"' + (opp.Risks_and_Potential_Mitigation_Actions__c != null && opp.Risks_and_Potential_Mitigation_Actions__c != '' ? opp.Risks_and_Potential_Mitigation_Actions__c : '') + '",';
                            jsonBody += '"Timeline__c":"' + (opp.Timeline__c != null && opp.Timeline__c != '' ? opp.Timeline__c : '') + '",';
                            jsonBody += '"Sector_NIDLP__c":"' + (opp.Sector__c != null && opp.Sector__c != '' ? opp.Sector__c : '') + '",';
                            jsonBody += '"Incentives_package__c":"' + (opp.Incentives_Package__c != null && opp.Incentives_Package__c != '' ? opp.Incentives_Package__c : '') + '",';
                            
                            // transform the location 1, location 2 and location 3 fields into a single field
                            string combinedLocation = '';
                            if(string.isNotBlank(opp.Location_1__c))
                            {
                                combinedLocation += opp.Location_1__c + ',';                    
                            }
                            if(string.isNotBlank(opp.Location_2__c))
                            {
                                combinedLocation += opp.Location_2__c + ',';
                            }
                            if(string.isNotBlank(opp.Location_3__c))
                            {
                                combinedLocation += opp.Location_3__c;
                            }
                            
                            // asssign combined location to wrapper location variable
                            jsonBody += '"Location__c":"' + (combinedLocation != null && combinedLocation != '' ? combinedLocation : '') + '"';
                            jsonBody += '}';
                            system.debug('Taha'+ jsonBody);
                            
                            // call the service to send opporutnity data
                            calloutToORG(oppID,jsonBody);
        
                        }
        
                        catch(Exception ex)
                        {
                            system.debug(ex.getMessage()+ ', line: ' + ex.getLineNumber() + ', cause: ' + ex.getCause() + ', type: '+ ex.getTypeName());
                            throw new AuraHandledException('Invalid Opportunity Details.');
                        }
                    }
                    else
                    {
                        system.debug('Taha' + sast + sast.ORG_Value__c);
                        throw new AuraHandledException('Error: Opportunity Stage should fulfill minimum criteria to send to ORG');
                    }
                    
                }
                
            }    
            else 
            {
                system.debug('Taha' + oppID);
                throw new AuraHandledException('Error: Opportunity Details not found.');    
            }
        }
        else
        {
            system.debug('Taha' + qaas);
            throw new AuraHandledException('Error: You are not authorized to perform this action.'); 
        }
    }

    //Method to get the access token from ORG
    @AuraEnabled 
    public static TokenWrapper getAccessToken()
    {
        try
        {
            // first get the custom setting values for integration configuration           
            config = ORG_Integration_Settings__c.getValues('ORG Settings');
            TokenWrapper tokenResponseWrap  = new TokenWrapper();
            string endpoint = '';

            if(config != null)
            {
                // prepare the token callout url
                endpoint = config.Token_Endpoint_URL__c + '?grant_type=password&client_id=' + config.Consumer_Key__c + 
                           '&client_secret=' + config.Consumer_Secret__c + '&username=' + config.Username__c + 
                           '&password=' + config.Password__c;

                system.debug(endpoint);   
                
                HttpRequest request = new HttpRequest();
                request.setEndpoint(endpoint);
                request.setMethod('POST');            
                request.setHeader('Content-Type', 'application/json');             
                request.setTimeout(120000);
                Http http = new Http();
                HTTPResponse feedResponse = new HTTPResponse();
                if(!Test.isRunningTest())
                {
                    feedResponse = http.send(request);
                	system.debug(feedResponse.getBody());
                    if(feedResponse != null && feedResponse.getStatusCode() == 200)
                    {
                        system.debug(feedResponse.getBody());
                        tokenResponseWrap = (TokenWrapper)System.JSON.deserializeStrict(feedResponse.getBody(), TokenWrapper.class);  
                        system.debug(tokenResponseWrap);    
                    }
                }
                else
                {
                    feedResponse.setBody('{"access_token":"SESSION_ID_REMOVED","instance_url":"https://sagia--test.my.salesforce.com","id":"https://test.salesforce.com/id/00D0E000000E0B8UAK/0050E000007WD43QAG","token_type":"Bearer","issued_at":"1604421642219","signature":"RqM5Ob2geUpxAgH8tYB96t8HuRC92dfaRCUOdRK54D4="}');
                    feedResponse.setStatusCode(200);
                    if(feedResponse != null && feedResponse.getStatusCode() == 200)
                    {
                        system.debug(feedResponse.getBody());
                        tokenResponseWrap = (TokenWrapper)System.JSON.deserializeStrict(feedResponse.getBody(), TokenWrapper.class);  
                        system.debug(tokenResponseWrap);    
                    }
                }
            }

            else 
            {                
                throw new AuraHandledException('Error: Connection failed. Please try again later.');       
            }

            return tokenResponseWrap;
        }

        catch(Exception ex)
        {
            system.debug(ex.getMessage());
            return null;
        }

        
    }
    
    // Method to perform callout to ORG Instance
    @AuraEnabled 
    public static void calloutToORG(string oppID, string body)
    {
        try
        {
            // firstly get the access token
            TokenWrapper accessWrap = new TokenWrapper();
            ResponseWrapper resWrap = new ResponseWrapper();

            // Instance to update opportunity wth ORG Opportunity ID
            Opportunity oppUpdate = new Opportunity();
            // get the opportunity ID
            oppUpdate.ID = oppID;
            
            // get the access token for the callout
            accessWrap = getAccessToken();

            if(accessWrap != null && body != null)
            {
                // make callout
                HttpRequest request = new HttpRequest();
                // set the upsert call endpoint from custom setting variable
                request.setEndpoint(accessWrap.instance_url + config.Investment_Opportunity_Upsert_URL__c + config.ORG_Opportunity_Object_Name__c + '/' + config.ORG_Opportunity_External_Field__c + '/' + oppID + '/?_HttpMethod=PATCH');
                request.setMethod('POST');              
                request.setHeader('Content-Type', 'application/json');
                request.setHeader('Authorization', accessWrap.token_type + ' ' + accessWrap.access_token); 
                request.setBody(body);
                request.setTimeout(120000);
                Http http = new Http();
                HTTPResponse callResponse = new HTTPResponse();     
                if(!Test.isRunningTest())
                {
                    callResponse = http.send(request);
                    if(callResponse != null && callResponse.getStatusCode() == 200)
                    {
                        system.debug('callResponse: '+callResponse.getBody());  
                        resWrap = (ResponseWrapper)System.JSON.deserializeStrict(callResponse.getBody(), ResponseWrapper.class);    
                        
                        // get the record id from ORG and update it under the opportunity record for NIDLP
                        oppUpdate.ORG_Opportunity_ID__c = resWrap.ID;
                        update oppUpdate;
                    }
                    else 
                    {
                        throw new AuraHandledException('Error: Connection failed. Please try again later.');        
                    }
                }
                else
                {
                    callResponse.setBody('{"access_token":"SESSION_ID_REMOVED","instance_url":"https://sagia--test.my.salesforce.com","id":"https://test.salesforce.com/id/00D0E000000E0B8UAK/0050E000007WD43QAG","token_type":"Bearer","issued_at":"1604421642219","signature":"RqM5Ob2geUpxAgH8tYB96t8HuRC92dfaRCUOdRK54D4="}');
                    callResponse.setStatusCode(200);
                    if(callResponse != null && callResponse.getStatusCode() == 200)
                    {
                        system.debug('callResponse: '+callResponse.getBody());  
                        resWrap = (ResponseWrapper)System.JSON.deserializeStrict(callResponse.getBody(), ResponseWrapper.class);    
                        system.debug('resWrap: '+resWrap);
                        // get the record id from ORG and update it under the opportunity record for NIDLP
                        oppUpdate.ORG_Opportunity_ID__c = resWrap.ID;
                        update oppUpdate;
                    }
                } 
            }

            else 
            {
                throw new AuraHandledException('Error: Connection failed. Please try again later.');        
            }

        }
        catch(Exception ex)
        {
            system.debug(ex.getMessage());           
        }
    }
    

    // Wrapper class to store token response 
    public class TokenWrapper
	{
		@AuraEnabled public string access_token;
        @AuraEnabled public string instance_url;
        @AuraEnabled public string id;
        @AuraEnabled public string signature;
        @AuraEnabled public string issued_at;
		@AuraEnabled public string token_type;
		
		public void TokenWrapper()
		{
			access_token = '';			
            token_type = '';
            instance_url = '';
            id = '';
            signature = '';
            issued_at = '';
		}
    }
    
    //Wrapper class to store the opportunity record response from ORG
    public class ResponseWrapper
	{
		@AuraEnabled public string id;
        @AuraEnabled public boolean success;
        @AuraEnabled public list<string> errors;
        @AuraEnabled public boolean created;
       
		public void ResponseWrapper()
		{
			id = '';			
            success = false;
            errors = new list<string>();
            created = false;         
		}
	}

}
