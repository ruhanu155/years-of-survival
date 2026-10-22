#!/usr/bin/env bash
# Director's stack merger. Usage: stackmerge.sh prepare N   (merge origin/main into PR N's branch, guard, push)
#                                 stackmerge.sh status  N   (CI + mergeable state)
#                                 stackmerge.sh merge   N   (squash-merge when green+clean, pull main, check no late commits)
set -e
R=ruhanu155/years-of-survival; cd /home/claude/years-of-survival
N=$2; br=$(gh api repos/$R/pulls/$N --jq .head.ref)
case "$1" in
prepare)
  git fetch -q origin
  wt=/home/claude/wt-pr$N
  [ -d $wt ] || git worktree add -q $wt -B $br origin/$br
  cd $wt && git checkout -q $br && git reset -q --hard origin/$br
  if git merge -q origin/main -m "Merge origin/main into $br" 2>/tmp/claude-0/merge$N.err; then echo "clean merge"; else
    echo "CONFLICTS (keeping branch side; stacked PR):"; git diff --name-only --diff-filter=U
    python3 - <<'PY'
import subprocess,re,sys,json
br=subprocess.check_output(['git','rev-parse','--abbrev-ref','HEAD']).decode().strip()
def union(a,b):
    if isinstance(a,dict) and isinstance(b,dict):
        o=dict(a)
        for k,v in b.items(): o[k]=union(o[k],v) if k in o else v
        return o
    if isinstance(a,list) and isinstance(b,list):
        o=list(a); [o.append(x) for x in b if x not in o]; return o
    return b
for f in subprocess.check_output(['git','diff','--name-only','--diff-filter=U']).decode().split():
    s=open(f).read()
    if f.endswith('inventory.json') or f.endswith('credits.json') or f.endswith('en_us.json'): continue
    if f.endswith('.json'):
        try:
            m=json.loads(subprocess.check_output(['git','show','origin/main:'+f])); b=json.loads(subprocess.check_output(['git','show','origin/'+br+':'+f]))
            json.dump(union(m,b),open(f,'w'),indent=2,ensure_ascii=False); print('json-union',f); continue
        except Exception as e: print('json-union failed',f,e)
    out=re.sub(r'<<<<<<< [^\n]*\n(.*?)=======\n.*?>>>>>>> [^\n]*\n', r'\1', s, flags=re.S)
    assert '<<<<<<<' not in out, f
    open(f,'w').write(out)
PY
    python3 - <<'PY'
import json
p='studio/inventory-rules.json'; d=json.load(open(p)); seen=set(); out=[]
for r in d['additions']:
    k=(r.get('type'),tuple(r.get('ids',[])))
    if k not in seen: seen.add(k); out.append(r)
d['additions']=out; json.dump(d,open(p,'w'),indent=2)
PY
    python3 - <<'PY'
import json,subprocess
p='src/main/resources/assets/yearofice/credits.json'
mine=json.load(open(p)); main=json.loads(subprocess.check_output(['git','show','origin/main:'+p]))
key=lambda e: e.get('asset_id') or tuple(e.get('files',[]))
by={key(e):e for e in mine['entries']}
for e in main['entries']:
    k=key(e)
    if k in by:
        f=by[k].setdefault('files',[]); [f.append(x) for x in e.get('files',[]) if x not in f]
    else: mine['entries'].append(e)
seen=set()
for e in mine['entries']:
    e['files']=[f for f in e.get('files',[]) if not (f in seen or seen.add(f))]
mine['entries']=[e for e in mine['entries'] if e.get('files')]
json.dump(mine,open(p,'w'),indent=2)
PY
    python3 - <<'PY'
import json,subprocess,re,os
p='src/main/resources/assets/yearofice/lang/en_us.json'
br=subprocess.check_output(['git','rev-parse','--abbrev-ref','HEAD']).decode().strip()
raw=subprocess.check_output(['git','show','origin/'+br+':'+p]).decode()
keys={m.group(1):json.loads('"'+m.group(2)+'"') for m in re.finditer(r'^\s*"([^"]+)"\s*:\s*"((?:[^"\\]|\\.)*)"', raw, flags=re.M)}
main=json.loads(subprocess.check_output(['git','show','origin/main:'+p]))
u=dict(main); u.update(keys); json.dump(u,open(p,'w'),indent=2,ensure_ascii=False)
PY
    python3 - <<'PY'
import subprocess,glob
br=subprocess.check_output(['git','rev-parse','--abbrev-ref','HEAD']).decode().strip()
for f in glob.glob('src/test/resources/saves/*/stacks.snbt'):
    try: main=subprocess.check_output(['git','show','origin/main:'+f],stderr=subprocess.DEVNULL).decode()
    except Exception: continue
    mine=subprocess.check_output(['git','show','origin/'+br+':'+f]).decode()
    ent=lambda s:[l.strip().rstrip(',') for l in s.strip().strip('[]').split('\n') if l.strip().startswith('{')]
    out=ent(main); [out.append(e) for e in ent(mine) if e not in out]
    open(f,'w').write('[\n'+',\n'.join('  '+e for e in out)+'\n]\n')
PY
    git show origin/$br:studio/inventory.json > studio/inventory.json; ./gradlew --offline docInventory -q
    git add -A && git commit -q -m "Merge origin/main into $br (stack: branch side)"
  fi
  ./gradlew --offline compileJava compileTestJava docInventory contentLint -q && ./gradlew --offline test --tests "*InventoryTest*" --tests "*SaveFixtures*" -q 2>&1 | grep -vE "^Note:|^Picked up" | tail -5
  # guards
  d=$(git diff --diff-filter=D --name-only origin/main...HEAD | grep -v '^$' || true)
  [ -z "$d" ] || { echo "DELETIONS vs main:"; echo "$d"; }
  python3 - <<'PY'
import json,glob,sys
bad=[f for f in glob.glob('src/main/resources/**/*.json',recursive=True) if (lambda f: (lambda s: ('<<<<<<<' in s) or (json.loads(s) is None))(open(f).read()))(f) is None and False]
PY
  python3 -c "
import json,glob,sys
bad=[]
for f in glob.glob('src/main/resources/**/*.json',recursive=True):
    s=open(f).read()
    if '<<<<<<<' in s: bad.append(f); continue
    try: json.loads(s)
    except Exception as e: bad.append(f)
print('JSON bad:',bad) if bad else print('JSON ok')
sys.exit(1 if bad else 0)"
  git push -q origin $br && echo "pushed $(git rev-parse --short HEAD)";;
status)
  sha=$(gh api repos/$R/pulls/$N --jq .head.sha)
  echo "$N $br $(gh api repos/$R/pulls/$N --jq .mergeable_state) $(gh api repos/$R/commits/$sha/check-runs --jq '[.check_runs[]|.name+":"+(.conclusion//.status)]|join(" ")')";;
merge)
  sha=$(gh api repos/$R/pulls/$N --jq .head.sha)
  c=$(gh api repos/$R/commits/$sha/check-runs --jq '[.check_runs[]|.conclusion]|map(select(.!="skipped"))|unique|join(",")')
  m=$(gh api repos/$R/pulls/$N --jq .mergeable_state)
  [ "$c" = "success" ] && [ "$m" = "clean" ] || { echo "not ready: checks=$c state=$m"; exit 3; }
  gh api -X PUT repos/$R/pulls/$N/merge -f merge_method=squash --jq .merged
  git fetch -q origin && git pull -q --ff-only
  late=$(git diff origin/main origin/$br --stat | wc -l); echo "content left unmerged (lines): $late"
  [ -d /home/claude/wt-pr$N ] && git worktree remove --force /home/claude/wt-pr$N || true
  t=$(gh api repos/$R/pulls/$N --jq .title)
  echo "$(($(grep -oE '^[0-9]+' /tmp/claude-0/-home-claude/3dd9ded9-61c9-50f0-89ad-cfeae0edf5c6/scratchpad/v2/RECONCILE.md | tail -1)+1)). MERGED PR #$N $t (Director)." >> /tmp/claude-0/-home-claude/3dd9ded9-61c9-50f0-89ad-cfeae0edf5c6/scratchpad/v2/RECONCILE.md;;
esac
# ---- rebuild: stackmerge.sh rebuild N sha1 [sha2...]  -> cherry-picks the PR's own commits onto origin/main as <branch>-rebased, opens a new PR, closes N
if [ "$1" = "rebuild" ]; then
  shift; N=$1; shift; R=ruhanu155/years-of-survival; cd /home/claude/years-of-survival; git fetch -q origin
  br=$(gh api repos/$R/pulls/$N --jq .head.ref); t=$(gh api repos/$R/pulls/$N --jq .title); nb="${br}-rebased"; wt=/home/claude/wt-rb$N
  git worktree remove --force $wt 2>/dev/null || true; git branch -D $nb 2>/dev/null || true; git worktree add -q $wt -b $nb origin/main; cd $wt
  for c in "$@"; do
    if ! git cherry-pick -x $c >/dev/null 2>&1; then
      echo "conflicts in $c:"; git diff --name-only --diff-filter=U
      python3 - "$c" <<'PY'
import json,subprocess,re,sys,glob
c=sys.argv[1]
def union(a,b):
    if isinstance(a,dict) and isinstance(b,dict):
        o=dict(a)
        for k,v in b.items(): o[k]=union(o[k],v) if k in o else v
        return o
    if isinstance(a,list) and isinstance(b,list):
        o=list(a); [o.append(x) for x in b if x not in o]; return o
    return b
for f in subprocess.check_output(['git','diff','--name-only','--diff-filter=U']).decode().split():
    try: ours=subprocess.check_output(['git','show','HEAD:'+f],stderr=subprocess.DEVNULL).decode()
    except Exception: ours=None
    try: theirs=subprocess.check_output(['git','show',c+':'+f],stderr=subprocess.DEVNULL).decode()
    except Exception: theirs=None
    if f.endswith('credits.json') and ours and theirs:
        a=json.loads(ours); b=json.loads(theirs); key=lambda e: e.get('asset_id') or tuple(e.get('files',[]))
        by={key(e):e for e in a['entries']}
        for e in b['entries']:
            k=key(e)
            if k in by:
                fl=by[k].setdefault('files',[]); [fl.append(x) for x in e.get('files',[]) if x not in fl]
            else: a['entries'].append(e); by[k]=e
        seen=set()
        for e in a['entries']: e['files']=[x for x in e.get('files',[]) if not (x in seen or seen.add(x))]
        a['entries']=[e for e in a['entries'] if e.get('files')]
        json.dump(a,open(f,'w'),indent=2); print('credits-union',f); continue
    if f.endswith('en_us.json') and ours and theirs:
        keys=lambda raw:{m.group(1):json.loads('"'+m.group(2)+'"') for m in re.finditer(r'^\s*"([^"]+)"\s*:\s*"((?:[^"\\]|\\.)*)"', raw, flags=re.M)}
        u=keys(ours); u.update(keys(theirs)); json.dump(u,open(f,'w'),indent=2,ensure_ascii=False); print('lang-union',f); continue
    if f.endswith('stacks.snbt') and ours and theirs:
        ent=lambda s:[l.strip().rstrip(',') for l in s.strip().strip('[]').split('\n') if l.strip().startswith('{')]
        out=ent(ours); [out.append(e) for e in ent(theirs) if e not in out]
        open(f,'w').write('[\n'+',\n'.join('  '+e for e in out)+'\n]\n'); print('snbt-union',f); continue
    if f.endswith('inventory.json'): open(f,'w').write(ours or theirs or '{}'); print('inventory-regen-later',f); continue
    if f.endswith('.json') and ours and theirs:
        try: json.dump(union(json.loads(ours),json.loads(theirs)),open(f,'w'),indent=2,ensure_ascii=False); print('json-union',f); continue
        except Exception as e: print('json-union failed',f,e)
    s=open(f).read()
    out=re.sub(r'<<<<<<< [^\n]*\n.*?=======\n(.*?)>>>>>>> [^\n]*\n', r'\1', s, flags=re.S)   # Java/other: take the cherry-picked (PR) side
    if '<<<<<<<' in out: print('LEFT',f); continue
    open(f,'w').write(out); print('pr-side',f)
PY
      python3 - <<'PY'
import json
p='studio/inventory-rules.json'; d=json.load(open(p)); seen=set(); out=[]
for r in d['additions']:
    k=(r.get('type'),tuple(r.get('ids',[])))
    if k not in seen: seen.add(k); out.append(r)
d['additions']=out; json.dump(d,open(p,'w'),indent=2)
PY
      ./gradlew --offline docInventory -q 2>&1 | grep -E "problem|error" | head -3
      git add -A && GIT_EDITOR=true git cherry-pick --continue >/dev/null || { echo "cherry-pick $c still conflicted"; exit 2; }
    fi
  done
  ./gradlew --offline docInventory contentLint compileJava compileTestJava -q 2>&1 | grep -vE "^Note|^Picked up" | grep -E "error|problem" | head -5
  ./gradlew --offline test --tests '*InventoryTest*' --tests '*SaveFixtures*' -q 2>&1 | grep -c FAILED
  git push -q -u origin $nb 2>/dev/null; gh api -X POST repos/$R/pulls -f title="$t (rebased)" -f head=$nb -f base=main -f body="Rebuilt on current main from PR #$N's own commits (its branch carried the whole stack). Same content.

🤖 Generated with [Claude Code](https://claude.com/claude-code)

https://claude.ai/code/session_01Xr2CVpzoZ636TDaUZHoGmj" --jq '"new PR #"+(.number|tostring)' && gh api -X PATCH repos/$R/pulls/$N -f state=closed --jq '"closed #'$N' "+.state'
fi
