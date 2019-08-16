package models.entities

//    id: ({ _ensgId, id }, args, { targetLoader }) =>
//      id ? id : targetLoader.load(_ensgId).then(({ id }) => id),
//    uniprotId: ({ _ensgId, id }, args, { targetLoader }) =>
//      id
//        ? id
//        : targetLoader.load(_ensgId).then(({ protein }) => protein.uniprotId),
//    symbol: ({ _ensgId, symbol }, args, { targetLoader }) =>
//      symbol ? symbol : targetLoader.load(_ensgId).then(({ symbol }) => symbol),
//    name: ({ _ensgId, name }, args, { targetLoader }) =>
//      name ? name : targetLoader.load(_ensgId).then(({ name }) => name),
//    description: ({ _ensgId, description }, args, { targetLoader }) =>
//      description
//        ? description
//        : targetLoader.load(_ensgId).then(({ description }) => description),
//    synonyms: ({ _ensgId, synonyms }, args, { targetLoader }) =>
//      synonyms
//        ? synonyms
//        : targetLoader.load(_ensgId).then(({ synonyms }) => synonyms),
//    summaries: _.identity,
//    details: _.identity,

case class Target(id: String, approvedSymbol: String, approvedName: String)
