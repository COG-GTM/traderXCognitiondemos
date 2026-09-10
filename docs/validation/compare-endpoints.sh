#!/usr/bin/env bash
# Behavioural snapshot of TraderX Java services.
# Usage: ./compare-endpoints.sh <label>   -> writes $VALIDATION_DIR/<label>/ (default /tmp/traderx-validation) and a normalised summary.
# Assumes database(18082), reference-data(18085), trade-feed(18086), account(18088),
# trade-processor(18091), trade-service(18092), position-service(18090) are running locally.
set -u
LABEL=$1
OUT=${VALIDATION_DIR:-/tmp/traderx-validation}/$LABEL
mkdir -p "$OUT"
SUMMARY=$OUT/summary.txt
: > "$SUMMARY"

norm() { # strip volatile fields: ids/uuids/timestamps
  python3 -c '
import sys,json,re
raw=sys.stdin.read()
try:
    d=json.loads(raw)
except Exception:
    print(re.sub(r"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}","<uuid>",raw).strip()); sys.exit()
VOL={"created","updated","id","timestamp","time"}
def scrub(o):
    if isinstance(o,dict):
        return {k:("<volatile>" if k.lower() in VOL and not isinstance(v,(dict,list)) else scrub(v)) for k,v in sorted(o.items())}
    if isinstance(o,list):
        return [scrub(x) for x in o]
    return o
print(json.dumps(scrub(d),sort_keys=True,separators=(",",":")))'
}

req() { # name method url [body]
  local name=$1 method=$2 url=$3 body=${4:-}
  local code
  if [ -n "$body" ]; then
    code=$(curl -s -o "$OUT/$name.raw" -w '%{http_code}' -X "$method" -H 'Content-Type: application/json' -d "$body" "$url")
  else
    code=$(curl -s -o "$OUT/$name.raw" -w '%{http_code}' -X "$method" "$url")
  fi
  if [ "$code" = "000" ]; then
    echo "ERROR: $name $method $url unreachable (curl failed); aborting snapshot" >&2
    exit 1
  fi
  echo "### $name $method $url" >> "$SUMMARY"
  echo "status=$code" >> "$SUMMARY"
  norm < "$OUT/$name.raw" >> "$SUMMARY"
  echo >> "$SUMMARY"
}

ACC=http://localhost:18088
POS=http://localhost:18090
TP=http://localhost:18091
TS=http://localhost:18092
REF=http://localhost:18085

# reference data (Node) - unchanged service, used by trade-service validation
req ref_stocks_list  GET "$REF/stocks" ; sed -i '$d' "$SUMMARY"; echo "(body sha256 $(sha256sum <"$OUT/ref_stocks_list.raw" | cut -c1-16))" >> "$SUMMARY"; echo >> "$SUMMARY"
req ref_stock_msft   GET "$REF/stocks/MSFT"
req ref_stock_bad    GET "$REF/stocks/NOPE"

# account-service
req acc_list         GET "$ACC/account/"
req acc_get_22214    GET "$ACC/account/22214"
req acc_get_missing  GET "$ACC/account/999999"
req acc_get_bad_id   GET "$ACC/account/notanint"
req acc_dblslash     GET "$ACC//account/22214"          # trade-service calls account-service with a double slash
req acc_trailing_dot GET "$ACC/account/22214/."
req acc_dotdot       GET "$ACC/account/22214/../22214"
req acc_encoded_dots GET "$ACC/account/%2e%2e/account/22214"
req acc_root_redir   GET "$ACC/"
req acc_swagger_json GET "$ACC/v3/api-docs" ; sed -i '$d' "$SUMMARY"; python3 -c "import json;d=json.load(open('$OUT/acc_swagger_json.raw'));print('paths=',sorted(d['paths']))" >> "$SUMMARY"; echo >> "$SUMMARY"
req acc_swagger_ui   GET "$ACC/swagger-ui/index.html" ; sed -i '$d' "$SUMMARY"; echo "(html length $(wc -c <"$OUT/acc_swagger_ui.raw"))" >> "$SUMMARY"; echo >> "$SUMMARY"
req acc_user_list    GET "$ACC/accountuser/"
req acc_user_get     GET "$ACC/accountuser/22214"
req acc_create       POST "$ACC/account/" '{"displayName":"Validation Account"}'
NEWID=$(python3 -c "import json;print(json.load(open('$OUT/acc_create.raw'))['id'])")
req acc_get_created  GET "$ACC/account/$NEWID"
req acc_update       PUT "$ACC/account/" "{\"id\":$NEWID,\"displayName\":\"Validation Account v2\"}"
req acc_get_updated  GET "$ACC/account/$NEWID"
req acc_bad_json     POST "$ACC/account/" '{not json'
req acc_static_trav  GET "$ACC/static/..%2f..%2fapplication.properties"
req acc_webjars_trav GET "$ACC/webjars/..%2f..%2f..%2fapplication.properties"

# position-service (before any trade)
req pos_list_initial GET "$POS/positions/"
req pos_acct_initial GET "$POS/positions/22214"
req trd_acct_initial GET "$POS/trades/22214"
req pos_bad_id       GET "$POS/positions/notanint"
req pos_dblslash     GET "$POS//positions/22214"
req pos_root_redir   GET "$POS/"

# trade-service -> validates ticker with reference-data and account with account-service, then publishes to trade-feed
req ts_root_redir    GET "$TS/"
req ts_bad_ticker    POST "$TS/trade/" '{"accountId":22214,"security":"NOPE","quantity":10,"side":"Buy"}'
req ts_bad_account   POST "$TS/trade/" '{"accountId":999999,"security":"MSFT","quantity":10,"side":"Buy"}'
req ts_submit_buy    POST "$TS/trade/" '{"accountId":22214,"security":"MSFT","quantity":100,"side":"Buy"}'
sleep 3   # let trade-processor consume via trade-feed
req ts_submit_sell   POST "$TS/trade/" '{"accountId":22214,"security":"MSFT","quantity":40,"side":"Sell"}'
sleep 3

# trade-processor direct REST entry point
req tp_order_direct  POST "$TP/tradeservice/order" '{"accountId":22214,"security":"AAPL","quantity":25,"side":"Buy"}'
req tp_root_redir    GET "$TP/"
sleep 2

# position-service after trades (state produced by trade-processor via feed + direct call)
req pos_acct_after   GET "$POS/positions/22214"
req trd_acct_after   GET "$POS/trades/22214"
req pos_list_after   GET "$POS/positions/"

echo "written $SUMMARY"
