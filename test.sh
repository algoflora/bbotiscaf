#!/bin/bash

# Define the two target addresses
ADDRESS1="https://a7wyc22q5d.execute-api.ap-southeast-1.amazonaws.com/test-1"
ADDRESS2="https://a7wyc22q5d.execute-api.ap-southeast-1.amazonaws.com/test-2"

# Function to send a POST request
send_request() {
    local address=$1
    local timestamp=$(date +%s | cut -b1-13)  # Current timestamp in milliseconds
    local body="{\"action\":{\"type\":\"greet\",\"arguments\":{\"name\":$timestamp}}}"
    
    curl -X POST -H "Content-Type: application/json" -d "$body" "$address"
    echo "Request sent to $address"
}

# Main loop to send 10 requests
for i in {1..10}
do
    # Randomly choose between the two addresses
    # if [ $((RANDOM % 2)) -eq 0 ]; then
    #     address=$ADDRESS1
    # else
    #     address=$ADDRESS2
    # fi

    # Send the request
    send_request "$ADDRESS1"
    send_request "$ADDRESS2"

    # Wait for a random interval between 0-1 seconds
    sleep $(awk -v min=0 -v max=1 'BEGIN{srand(); print min+rand()*(max-min)}')
done

echo "All requests sent."
