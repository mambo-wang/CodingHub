import{f as k,j as D,p as t,e as n,a as o,n as N,i as d,w as a,h as y,F as B,q as E,m as M,v as w,_ as F,x as A,u as x,o as I,d as P,G as R,B as q,H as G,r as z,c as L,z as K,b as H}from"./index-DiqYniOn.js";import{u as U}from"./forum-6ZGkb73y.js";import{L as J,S as O,P as Q}from"./SidebarNav-C-bc_JiV.js";import"./index-DcNlVx-A.js";import"./api-DjM_YNK0.js";import"./user-DTZbcWT4.js";import"./heart-BE3p4idI.js";/**
 * @license @lucide/vue v1.17.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const W=[["path",{d:"m15 18-6-6 6-6",key:"1wnfg3"}]],X=k("chevron-left",W);/**
 * @license @lucide/vue v1.17.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const Y=[["path",{d:"m9 18 6-6-6-6",key:"mthhwq"}]],Z=k("chevron-right",Y);/**
 * @license @lucide/vue v1.17.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const ee=[["path",{d:"M5 12h14",key:"1ays0h"}],["path",{d:"M12 5v14",key:"s699le"}]],te=k("plus",ee);/**
 * @license @lucide/vue v1.17.0 - ISC
 *
 * This source code is licensed under the ISC license.
 * See the LICENSE file in the root directory of this source tree.
 */const se=[["path",{d:"m21 21-4.34-4.34",key:"14j7rj"}],["circle",{cx:"11",cy:"11",r:"8",key:"4ej97u"}]],oe=k("search",se),ae={class:"category-filter"},ne=["onClick"],ie=D({__name:"CategoryFilter",props:{categories:{},selectedCategory:{}},emits:["select"],setup(v,{emit:C}){const i=C,_={1:"#7C3AED",2:"#2563EB",3:"#059669",4:"#DC2626",5:"#D97706"},g=l=>_[l]||"#7C3AED",h=l=>{i("select",l)};return(l,u)=>(t(),n("div",ae,[o("button",{class:N(["filter-btn",{active:!v.selectedCategory}]),onClick:u[0]||(u[0]=s=>h(null))},[d(a(J),{size:14}),u[1]||(u[1]=y(" 全部 ",-1))],2),(t(!0),n(B,null,E(v.categories,s=>(t(),n("button",{key:s.id,class:N(["filter-btn",{active:v.selectedCategory===s.id}]),style:M(v.selectedCategory===s.id?{background:g(s.id),borderColor:g(s.id)}:{}),onClick:p=>h(s.id)},w(s.name),15,ne))),128))]))}}),le=F(ie,[["__scopeId","data-v-4ae24b54"]]),re={class:"post-list-page"},ce={class:"main-content"},de={class:"page-header"},ue={class:"search-bar"},ve={key:0,class:"loading"},ge={key:1,class:"post-list"},pe={key:0,class:"empty"},me={key:2,class:"pagination"},ye=["disabled"],Ce=["disabled"],he=D({__name:"PostListPage",setup(v){const C=K(),i=U(),_=A(),{posts:g,categories:h,pagination:l,loading:u}=x(i),{isLoggedIn:s}=x(_),p=z(""),f=z(null),m=L(()=>l.value.page),b=L(()=>l.value.totalPages);I(async()=>{await Promise.all([i.fetchPosts(),i.fetchCategories()])});const T=async r=>{f.value=r,await i.fetchPosts({category:r??void 0,page:0})},S=async()=>{await i.fetchPosts({keyword:p.value||void 0,page:0})},$=async r=>{await i.fetchPosts({category:f.value??void 0,keyword:p.value||void 0,page:r})},V=r=>{C.push(`/forum/posts/${r}`)},j=()=>{C.push("/forum/editor")};return(r,e)=>(t(),n("div",re,[d(O),o("div",ce,[o("div",de,[e[4]||(e[4]=o("h1",null,"论坛",-1)),a(s)?(t(),n("button",{key:0,onClick:j,class:"create-btn"},[d(a(te),{size:16}),e[3]||(e[3]=y(" 发布帖子 ",-1))])):P("",!0)]),d(le,{categories:a(h),selectedCategory:f.value,onSelect:T},null,8,["categories","selectedCategory"]),o("div",ue,[R(o("input",{"onUpdate:modelValue":e[0]||(e[0]=c=>p.value=c),onKeydown:G(S,["enter"]),placeholder:"搜索帖子标题..."},null,544),[[q,p.value]]),o("button",{onClick:S},[d(a(oe),{size:16}),e[5]||(e[5]=y(" 搜索 ",-1))])]),a(u)?(t(),n("div",ve,"加载中...")):(t(),n("div",ge,[(t(!0),n(B,null,E(a(g),c=>(t(),H(Q,{key:c.id,post:c,onClick:ke=>V(c.id)},null,8,["post","onClick"]))),128)),a(g).length===0?(t(),n("div",pe," 暂无帖子 ")):P("",!0)])),b.value>1?(t(),n("div",me,[o("button",{onClick:e[1]||(e[1]=c=>$(m.value-1)),disabled:m.value===0},[d(a(X),{size:16}),e[6]||(e[6]=y(" 上一页 ",-1))],8,ye),o("span",null,w(m.value+1)+" / "+w(b.value),1),o("button",{onClick:e[2]||(e[2]=c=>$(m.value+1)),disabled:m.value>=b.value-1},[e[7]||(e[7]=y(" 下一页 ",-1)),d(a(Z),{size:16})],8,Ce)])):P("",!0)])]))}}),Ne=F(he,[["__scopeId","data-v-8844394c"]]);export{Ne as default};
