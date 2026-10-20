# Generated the M5 plant, crop and lick art and data rows (original pixel art; run from anywhere). Rerun only to regenerate untracked files.
import json, os, random, re, sys
from PIL import Image
import os
HERE=os.path.dirname(os.path.abspath(__file__))
exec(open(HERE+'/table.py').read())
ROOT=os.path.abspath(HERE+'/../../..')
DATA=ROOT+'/src/main/resources/data/yearofice'
AS=ROOT+'/src/main/resources/assets/yearofice'
os.makedirs(DATA+'/plant',exist_ok=True)
def hexrgb(h): return tuple(int(h[i:i+2],16) for i in (0,2,4))
BERRYISH={'berry'}
parts={}   # part -> row dict
specs={}
for r in R_:
    sid='yearofice:'+r['id']
    form=r['form']; f=r['fdd']
    ph={}
    if form=='berry':
        ph={'bud':0,'leaf':40,'flower':90,'fruit':f,'seed':f+220}
        harvest=['fruit']
    elif form=='fungus':
        ph={'leaf':0,'fruit':f or 300,'seed':(f or 300)+60}
        harvest=['fruit']
    elif form in('moss','lichen'):
        ph={'leaf':0}; harvest=['bud','leaf','flower','fruit','seed','senescent']
    elif form in('shrub',):
        ph={'bud':0,'leaf':50,'flower':60,'seed':350}; harvest=['bud','leaf','flower','fruit','seed']
    else:
        fl=f or 150
        ph={'bud':0,'leaf':30,'flower':fl,'fruit':fl+120,'seed':fl+250}
        harvest=['bud','leaf','flower']
    if form in('root',) or (r['verbs'] and 'dig' in r['verbs']): rootharvest=['bud','leaf','flower','fruit','seed','senescent']
    ph={k:float(v) for k,v in ph.items()}
    part=r['part']; item='yearofice:'+part if part else 'yearofice:plant_material'
    verbs={}
    prim='pick'
    if r['verbs'] and 'dig' in r['verbs']:
        d=r['verbs']['dig']
        verbs['dig']=dict(part='yearofice:'+d['part'],unit='root',unit_g=d['unit_g'],time_min=float(d['tmin']),units_per_clump=3.0,phases=rootharvest,stripped_weight=1.0)
        if part and part!=d['part']:
            verbs['pick']=dict(part=item,unit='handful',unit_g=r['unit_g'],time_min=float(r['tmin']),units_per_clump=r['units'],phases=harvest,stripped_weight=1.0)
        if part: parts[d['part']]=dict(r,part=d['part'],unit_g=d['unit_g'],kcal=d.get('kcal',r['kcal']),cd=0 if d['part']!=part else r['cd'])
    elif form=='berry' or form=='fungus' or form in('moss','lichen'):
        unit={'berry':'cup','fungus':'cap','moss':'sheet','lichen':'handful'}[form]
        verbs['pick']=dict(part=item,unit=unit,unit_g=r['unit_g'] if part else 100,time_min=float(r['tmin']),units_per_clump=r['units'],phases=harvest,stripped_weight=1.0)
        if form=='berry': verbs['strip']=dict(allowed=False,reason='berries_burst') if r['id'] in('soapberry_bush','cloudberry_bush','raspberry_bush') else dict(allowed=True,work_scale=0.4,stripped_weight=1.5)
        verbs['uproot']=dict(drops='minecraft:stick',empty_years=3)
    elif form in('shrub',) or not part:
        verbs['cut']=dict(part=item,unit='handful',unit_g=r['unit_g'] if part else 100,time_min=float(r['tmin']),units_per_clump=r['units'],phases=harvest,stripped_weight=0.5)
        verbs['uproot']=dict(drops='minecraft:stick',empty_years=3)
    else:
        verbs['pick']=dict(part=item,unit='handful',unit_g=r['unit_g'],time_min=float(r['tmin']),units_per_clump=r['units'],phases=harvest,stripped_weight=1.0)
        verbs['cut']=dict(part=item,unit='handful',unit_g=r['unit_g'],time_min=float(r['tmin']),units_per_clump=r['units'],phases=harvest,stripped_weight=0.5)
        verbs['uproot']=dict(drops='minecraft:stick',empty_years=1)
    if part and part not in parts and not (r['verbs'] and 'dig' in r['verbs'] and part==r['verbs']['dig']['part']): parts[part]=r
    if part and r['verbs'] and 'dig' in r['verbs'] and part!=r['verbs']['dig']['part']: parts[part]=r
    tags=list(r['tags'])
    if form=='berry' and r['id']!='wild_rose': tags.append('yearofice:berry_bushes')
    if r['tox']: tags.append('yearofice:toxic_plants')
    tags=sorted(set(tags))
    s=dict(id=sid, name=r['name'], form={'berry':'berry_shrub','herb':'herb','root':'herb','shrub':'shrub','fern':'fern','grass':'grass','aquatic':'aquatic','moss':'moss','lichen':'lichen','fungus':'mushroom'}[form],
        tags=tags, habitat=r['habitat'], phases_dd=dict(ph, senescent='first_frost'), verbs=verbs,
        patch=dict(clumps=list(r['patch']), regrow_days=list(r['regrow']), flush_mults=[1.0]),
        identify=dict(pocket_guide=bool(r['pocket']), notes=(['yearofice:'+r['note']] if r['note'] else []), stages=['shoot','flower','fruit']),
        inspect=dict(look=r['look'],smell=r['smell'],cut=r['cut']))
    if r['tox']:
        g,loss,lo,hi=r['tox']; s['toxic']=dict(grade=g,condition_loss=loss,onset_min=[lo,hi])
    if r['group']: s['lookalike_group']=r['group']
    if part and (r['kcal']>0 or r['cd']>0):
        s['food']=dict(part=item, kcal_per_100g=round(r['kcal']/r['unit_g']*100,1), vit_c_mg_per_100g=round(r['cd']*10/r['unit_g']*100,1), spoil_days_at_5c=r['spoil'])
    specs[r['id']]=s
for sid,s in specs.items():
    json.dump(s,open(f'{DATA}/plant/{sid}.json','w'),indent=2); open(f'{DATA}/plant/{sid}.json','a').write('\n')
ids=list(specs)
partlist=sorted(parts)
# Java catalog
java='''package com.yearofice.plant;

/** Generated list of the plant species blocks and their part items (the plant/*.json files hold the numbers). A JUnit test keeps it equal to the data folder. */
public final class PlantCatalog {
    private PlantCatalog() {}

    public static final String[] SPECIES = {
%s
    };

    /** Food and material part items named after their species (the generic {@code plant_material} is separate). */
    public static final String[] PARTS = {
%s
    };

    /** The parts that are food (eaten through the food data map). */
    public static final String[] FOOD_PARTS = {
%s
    };
}
'''%(',\n'.join('        "%s"'%i for i in ids), ',\n'.join('        "%s"'%i for i in partlist), ',\n'.join('        "%s"'%p for p in partlist if parts[p]['kcal']>0 or parts[p]['cd']>0))
open(ROOT+'/src/main/java/com/yearofice/plant/PlantCatalog.java','w').write(java)
# assets
for d in ('blockstates','models/block','models/item','textures/block','textures/item'): os.makedirs(f'{AS}/{d}',exist_ok=True)
def blob(img,rnd,cx,cy,rad,col):
    for x in range(16):
        for y in range(16):
            if (x-cx)**2+(y-cy)**2<=rad*rad and rnd.random()<0.9:
                v=rnd.randint(-14,14); img.putpixel((x,y),tuple(max(0,min(255,c+v)) for c in col)+(255,))
def tex_block(r,bare):
    rnd=random.Random(r['id']); col=hexrgb(r['color']); img=Image.new('RGBA',(16,16),(0,0,0,0))
    green=(70,120,60); form=r['form']
    if form=='moss' or form=='lichen':
        for x in range(2,14):
            for y in range(11,16):
                if rnd.random()<0.85: img.putpixel((x,y),tuple(max(0,min(255,c+rnd.randint(-12,12))) for c in col)+(255,))
        return img
    if form=='fungus':
        cx=8
        for y in range(9,16): img.putpixel((cx,y),(225,220,205,255)); img.putpixel((cx+1,y),(210,205,190,255))
        if not bare: blob(img,rnd,8,8,4,col)
        return img
    for x0 in (4,8,11):
        top=2+rnd.randint(0,4) if form!='berry' else 5+rnd.randint(0,3)
        for y in range(top,16): img.putpixel((x0,y),green+(255,))
    if form in('berry','shrub'):
        for _ in range(7):
            blob(img,rnd,rnd.randint(3,12),rnd.randint(5,13),1.5,(60,100,50) if bare else green)
    if not bare:
        n=9 if form=='berry' else 4
        for _ in range(n):
            blob(img,rnd,rnd.randint(2,13),rnd.randint(3,11),1.2,col)
    return img
def tex_item(color,key):
    rnd=random.Random('i'+key); col=hexrgb(color); img=Image.new('RGBA',(16,16),(0,0,0,0))
    for _ in range(3): blob(img,rnd,rnd.randint(5,10),rnd.randint(6,10),3,col)
    return img
for r in R_:
    i=r['id']
    tex_block(r,False).save(f'{AS}/textures/block/{i}.png'); tex_block(r,True).save(f'{AS}/textures/block/{i}_bare.png')
    for suf,t in (('',i),('_bare',i+'_bare')):
        json.dump({'parent':'minecraft:block/cross','textures':{'cross':f'yearofice:block/{t}'},'render_type':'minecraft:cutout'},open(f'{AS}/models/block/{i}{suf}.json','w'),indent=2)
    json.dump({'multipart':[{'when':{'taken':'0|1'},'apply':{'model':f'yearofice:block/{i}'}},{'when':{'taken':'2|3'},'apply':{'model':f'yearofice:block/{i}_bare'}}]},open(f'{AS}/blockstates/{i}.json','w'),indent=2)
import subprocess
tracked=set(subprocess.run(['git','-C',ROOT,'ls-files','src/main/resources/assets'],capture_output=True,text=True).stdout.split())
for p in partlist+['plant_material']:
    if 'src/main/resources/assets/yearofice/models/item/%s.json'%p in tracked: continue
    r=parts.get(p); col=r['color'] if r else '8a9a6a'
    tex_item(col,p).save(f'{AS}/textures/item/{p}.png')
    json.dump({'parent':'minecraft:item/generated','textures':{'layer0':f'yearofice:item/{p}'}},open(f'{AS}/models/item/{p}.json','w'),indent=2)
# food rows (inserted at the head of values)
fp=DATA+'/data_maps/item/food.json'
food=json.load(open(fp))
existing=set(food['values'])
new={}
for p in partlist:
    r=parts[p]
    if not (r['kcal']>0 or r['cd']>0): continue
    k='yearofice:'+p
    if k in existing: continue
    kc=r['kcal']/r['unit_g']*100
    row={"neoforge:conditions":[{"type":"neoforge:item_exists","item":k}],"kcal_per_100g":round(kc,1),
      "protein_g_per_100g":round(kc*r['protein_pct']/100/4,1),"fat_g_per_100g":round(kc*r['fat_pct']/100/9,1),
      "vit_c_mg_per_100g":round(r['cd']*10/r['unit_g']*100,1),"curve":"living_rot","weight_g":float(r['unit_g']),
      "spoil_days_at_5c":{"raw":float(r['spoil'])}}
    new[k]=row
txt=open(fp).read()
head='"values": {\n'
ins=''.join('    %s: %s,\n'%(json.dumps(k),json.dumps(v)) for k,v in new.items())
assert head in txt
open(fp,'w').write(txt.replace(head,head+ins,1))
json.dump(dict(species=ids,parts=partlist,newfood=list(new)),open(os.path.join(HERE,'out.json'),'w'))
print(len(ids),'species',len(partlist),'parts',len(new),'food rows')
