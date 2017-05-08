import React from 'react'
import ReactDOM from 'react-dom'

import Root from './containers/Root'

const render = Component => {
  ReactDOM.render(
      <Component />,
    document.getElementById('root')
  )
}

render(Root)